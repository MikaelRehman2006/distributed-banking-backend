package com.mikaelrehman.banking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mikaelrehman.banking.dto.AuthResponse;
import com.mikaelrehman.banking.dto.TransferRequest;
import com.mikaelrehman.banking.entity.Account;
import com.mikaelrehman.banking.repository.AccountRepository;
import com.mikaelrehman.banking.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransferIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    private String aliceToken;
    private Long aliceAccountId;
    private Long bobAccountId;

    @BeforeEach
    void setUp() throws Exception {
        AuthResponse alice = register("alice-" + UUID.randomUUID() + "@test.com");
        AuthResponse bob = register("bob-" + UUID.randomUUID() + "@test.com");

        aliceToken = alice.accessToken();
        aliceAccountId = alice.accountId();
        bobAccountId = bob.accountId();

        fund(aliceAccountId, new BigDecimal("1000.00"));
        fund(bobAccountId, new BigDecimal("500.00"));
    }

    @Test
    void transfer_updatesBalances_andSupportsIdempotentRetry() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        TransferRequest transfer = new TransferRequest(aliceAccountId, bobAccountId, new BigDecimal("100.00"));

        mockMvc.perform(post("/transfers")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(100.00));

        mockMvc.perform(get("/accounts/" + aliceAccountId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(900.00));

        mockMvc.perform(post("/transfers")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100.00));

        mockMvc.perform(get("/accounts/" + aliceAccountId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(900.00));

        Account bob = accountRepository.findById(bobAccountId).orElseThrow();
        assertThat(bob.getBalance()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    void transfer_rejectsInsufficientFunds() throws Exception {
        TransferRequest transfer = new TransferRequest(aliceAccountId, bobAccountId, new BigDecimal("5000.00"));

        mockMvc.perform(post("/transfers")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    private AuthResponse register(String email) throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.mikaelrehman.banking.dto.RegisterRequest(email, "password123"));

        String json = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(json, AuthResponse.class);
    }

    private void fund(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.credit(amount);
        accountRepository.save(account);
    }
}
