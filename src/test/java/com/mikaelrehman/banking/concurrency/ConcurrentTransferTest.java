package com.mikaelrehman.banking.concurrency;

import com.mikaelrehman.banking.dto.AuthResponse;
import com.mikaelrehman.banking.dto.RegisterRequest;
import com.mikaelrehman.banking.dto.TransferRequest;
import com.mikaelrehman.banking.entity.Account;
import com.mikaelrehman.banking.repository.AccountRepository;
import com.mikaelrehman.banking.service.TransferService;
import com.mikaelrehman.banking.support.AbstractPostgresIntegrationTest;
import com.mikaelrehman.banking.support.TestSecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConcurrentTransferTest extends AbstractPostgresIntegrationTest {

    private static final int THREAD_COUNT = 50;
    private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferService transferService;

    private Long senderUserId;
    private String senderEmail;
    private Long senderAccountId;
    private Long receiverAccountId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString();
        AuthResponse sender = register("sender-" + suffix + "@test.com");
        AuthResponse receiver = register("receiver-" + suffix + "@test.com");

        senderUserId = sender.userId();
        senderEmail = "sender-" + suffix + "@test.com";
        senderAccountId = sender.accountId();
        receiverAccountId = receiver.accountId();

        Account senderAccount = accountRepository.findById(senderAccountId).orElseThrow();
        senderAccount.credit(INITIAL_BALANCE);
        accountRepository.save(senderAccount);
    }

    @Test
    void concurrentTransfers_neverOverdraft_andPreserveTotalBalance() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            String idempotencyKey = "concurrent-" + UUID.randomUUID();
            TransferRequest request = new TransferRequest(senderAccountId, receiverAccountId, TRANSFER_AMOUNT);
            tasks.add(() -> TestSecurityContext.callAs(senderUserId, senderEmail, () -> {
                try {
                    transferService.executeTransfer(request, idempotencyKey);
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }));
        }

        List<Future<Boolean>> results = executor.invokeAll(tasks);
        executor.shutdown();

        int successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                successes++;
            }
        }

        Account sender = accountRepository.findById(senderAccountId).orElseThrow();
        Account receiver = accountRepository.findById(receiverAccountId).orElseThrow();

        BigDecimal expectedDebited = TRANSFER_AMOUNT.multiply(BigDecimal.valueOf(successes));

        assertThat(sender.getBalance()).isEqualByComparingTo(INITIAL_BALANCE.subtract(expectedDebited));
        assertThat(receiver.getBalance()).isEqualByComparingTo(expectedDebited);
        assertThat(sender.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(successes).isEqualTo(THREAD_COUNT);
    }

    private AuthResponse register(String email) throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterRequest(email, "password123"));
        String json = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(json, AuthResponse.class);
    }
}
