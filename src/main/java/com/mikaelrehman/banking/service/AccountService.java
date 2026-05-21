package com.mikaelrehman.banking.service;

import com.mikaelrehman.banking.dto.AccountResponse;
import com.mikaelrehman.banking.entity.Account;
import com.mikaelrehman.banking.exception.ForbiddenException;
import com.mikaelrehman.banking.exception.ResourceNotFoundException;
import com.mikaelrehman.banking.repository.AccountRepository;
import com.mikaelrehman.banking.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.getUserId().equals(SecurityUtils.currentUser().getUserId())) {
            throw new ForbiddenException("You do not own this account");
        }

        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getBalance(),
                account.getCurrency());
    }
}
