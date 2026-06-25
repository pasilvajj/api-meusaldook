package com.economy.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(
        long tokenExpirationMs, String frontendUrl, String mobileScheme) {}
