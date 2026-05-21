package com.mikaelrehman.banking.service;

import com.mikaelrehman.banking.dto.AccountResponse;
import com.mikaelrehman.banking.dto.DepositRequest;
import com.mikaelrehman.banking.entity.Account;
import com.mikaelrehman.banking.exception.ForbiddenException;
import com.mikaelrehman.banking.exception.ResourceNotFoundException;
import com.mikaelrehman.banking.repository.AccountRepository;
import com.mikaelrehman.banking.security.SecurityUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("dev")
public class DevAccountService {

    private final AccountRepository accountRepository;

    public DevAccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse deposit(Long accountId, DepositRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.getUserId().equals(SecurityUtils.currentUser().getUserId())) {
            throw new ForbiddenException("You do not own this account");
        }

        account.credit(request.amount());
        Account saved = accountRepository.save(account);

        return new AccountResponse(
                saved.getId(),
                saved.getUserId(),
                saved.getBalance(),
                saved.getCurrency());
    }
}
