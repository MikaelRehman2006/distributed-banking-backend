package com.mikaelrehman.banking.controller;

import com.mikaelrehman.banking.dto.AccountResponse;
import com.mikaelrehman.banking.dto.DepositRequest;
import com.mikaelrehman.banking.service.DevAccountService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/dev/accounts")
public class DevAccountController {

    private final DevAccountService devAccountService;

    public DevAccountController(DevAccountService devAccountService) {
        this.devAccountService = devAccountService;
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest request) {
        return devAccountService.deposit(id, request);
    }
}
