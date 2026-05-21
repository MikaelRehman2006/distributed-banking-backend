package com.mikaelrehman.banking.controller;

import com.mikaelrehman.banking.dto.TransferRequest;
import com.mikaelrehman.banking.dto.TransferResponse;
import com.mikaelrehman.banking.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return transferService.executeTransfer(request, idempotencyKey);
    }

    @GetMapping("/{id}")
    public TransferResponse getTransfer(@PathVariable Long id) {
        return transferService.getTransfer(id);
    }
}
