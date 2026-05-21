package com.mikaelrehman.banking.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static BankingUserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof BankingUserPrincipal principal)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return principal;
    }
}
