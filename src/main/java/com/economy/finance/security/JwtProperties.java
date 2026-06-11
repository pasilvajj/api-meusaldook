package com.economy.finance.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HS256 secret (min 256 bits / 32 bytes recommended). */
    private String secret = "change-me";

    private long expirationMs = 86_400_000L;
}
