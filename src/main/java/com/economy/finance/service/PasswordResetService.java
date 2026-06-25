package com.economy.finance.service;

import com.economy.finance.api.dto.ForgotPasswordRequest;
import com.economy.finance.api.dto.MessageResponse;
import com.economy.finance.api.dto.ResetPasswordRequest;
import com.economy.finance.config.PasswordResetProperties;
import com.economy.finance.domain.AppUser;
import com.economy.finance.domain.PasswordResetToken;
import com.economy.finance.persistence.AppUserRepository;
import com.economy.finance.persistence.PasswordResetTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String GENERIC_FORGOT_MESSAGE =
            "Se o email existir na nossa base, receberás instruções para redefinir a palavra-passe.";

    private final AppUserRepository appUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetProperties passwordResetProperties;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public MessageResponse requestReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        appUserRepository
                .findByEmailIgnoreCase(email)
                .ifPresent(
                        user -> {
                            passwordResetTokenRepository.deleteByUserId(user.getId());
                            String rawToken = generateRawToken();
                            Instant now = Instant.now();
                            PasswordResetToken entity =
                                    PasswordResetToken.builder()
                                            .user(user)
                                            .tokenHash(hashToken(rawToken))
                                            .createdAt(now)
                                            .expiresAt(
                                                    now.plusMillis(
                                                            passwordResetProperties.tokenExpirationMs()))
                                            .build();
                            passwordResetTokenRepository.save(entity);
                            emailService.sendPasswordResetEmail(
                                    user.getEmail(),
                                    buildWebResetLink(rawToken),
                                    buildMobileResetLink(rawToken));
                        });

        return MessageResponse.builder().message(GENERIC_FORGOT_MESSAGE).build();
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.getToken().trim());
        PasswordResetToken token =
                passwordResetTokenRepository
                        .findByTokenHashAndUsedAtIsNull(tokenHash)
                        .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Link inválido ou expirado. Pede um novo email de recuperação."));

        AppUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.deleteByUserId(user.getId());

        return MessageResponse.builder()
                .message("Palavra-passe atualizada com sucesso. Já podes iniciar sessão.")
                .build();
    }

    private String buildWebResetLink(String rawToken) {
        String base = trimTrailingSlash(passwordResetProperties.frontendUrl());
        return base + "/auth/reset-password?token=" + rawToken;
    }

    private String buildMobileResetLink(String rawToken) {
        return passwordResetProperties.mobileScheme()
                + "://reset-password?token="
                + rawToken;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:4200";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }
}
