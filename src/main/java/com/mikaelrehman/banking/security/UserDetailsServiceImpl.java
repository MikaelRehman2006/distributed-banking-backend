package com.mikaelrehman.banking.security;

import com.mikaelrehman.banking.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(user -> new BankingUserPrincipal(
                        user.getId(),
                        user.getEmail(),
                        user.getPasswordHash(),
                        user.getRole().name()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
