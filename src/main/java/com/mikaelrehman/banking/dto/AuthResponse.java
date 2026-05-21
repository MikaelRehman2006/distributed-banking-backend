package com.mikaelrehman.banking.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        Long userId,
        Long accountId
) {
}
