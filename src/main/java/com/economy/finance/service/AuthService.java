package com.economy.finance.service;

import com.economy.finance.api.dto.AuthResponse;
import com.economy.finance.api.dto.LoginRequest;
import com.economy.finance.api.dto.RegisterRequest;
import com.economy.finance.api.exception.ConflictException;
import com.economy.finance.api.exception.ResourceNotFoundException;
import com.economy.finance.domain.AppUser;
import com.economy.finance.persistence.AppUserRepository;
import com.economy.finance.security.JwtService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ConflictException("Email já registado");
        }
        AppUser user =
                AppUser.builder()
                        .email(request.getEmail().trim().toLowerCase())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .fullName(request.getFullName().trim())
                        .createdAt(Instant.now())
                        .build();
        user = appUserRepository.save(user);
        accountService.seedPrincipalIfMissing(user.getId());
        categoryService.seedDefaultsIfEmpty(user.getId());
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        AppUser user =
                appUserRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado"));
        return tokenFor(user);
    }

    private AuthResponse tokenFor(AppUser user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .tokenType("Bearer")
                .build();
    }
}
