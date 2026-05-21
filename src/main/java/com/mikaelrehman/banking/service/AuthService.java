package com.mikaelrehman.banking.service;

import com.mikaelrehman.banking.dto.AuthResponse;
import com.mikaelrehman.banking.dto.LoginRequest;
import com.mikaelrehman.banking.dto.RegisterRequest;
import com.mikaelrehman.banking.entity.Account;
import com.mikaelrehman.banking.entity.Role;
import com.mikaelrehman.banking.entity.User;
import com.mikaelrehman.banking.exception.BadRequestException;
import com.mikaelrehman.banking.repository.AccountRepository;
import com.mikaelrehman.banking.repository.UserRepository;
import com.mikaelrehman.banking.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = userRepository.save(new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER));

        Account account = accountRepository.save(new Account(user.getId(), BigDecimal.ZERO, "USD"));

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, "Bearer", jwtTokenProvider.getExpirationMs(), user.getId(), account.getId());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("User not found"));

        Account account = accountRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No account found for user"));

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, "Bearer", jwtTokenProvider.getExpirationMs(), user.getId(), account.getId());
    }
}
