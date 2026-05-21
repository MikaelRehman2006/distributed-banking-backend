package com.mikaelrehman.banking.support;

import com.mikaelrehman.banking.security.BankingUserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Callable;

public final class TestSecurityContext {

    private TestSecurityContext() {
    }

    public static void runAs(Long userId, String email, Runnable action) {
        callAs(userId, email, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T callAs(Long userId, String email, Callable<T> action) {
        BankingUserPrincipal principal = new BankingUserPrincipal(userId, email, "", "USER");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(context);
        try {
            return action.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
