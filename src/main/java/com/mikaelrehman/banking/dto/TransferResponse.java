package com.mikaelrehman.banking.dto;

import com.mikaelrehman.banking.entity.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long id,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        TransferStatus status,
        Instant createdAt
) {
}
