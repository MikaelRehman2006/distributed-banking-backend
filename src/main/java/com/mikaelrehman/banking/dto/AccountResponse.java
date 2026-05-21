package com.mikaelrehman.banking.dto;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        Long userId,
        BigDecimal balance,
        String currency
) {
}
