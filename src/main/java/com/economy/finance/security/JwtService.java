package com.economy.finance.security;

import com.economy.finance.domain.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    public String generateToken(AppUser user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + properties.getExpirationMs());
        return Jwts.builder()
                .claims(Map.of("uid", user.getId()))
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object uid = parseClaims(token).get("uid");
        if (uid instanceof Number) {
            return ((Number) uid).longValue();
        }
        if (uid instanceof String) {
            return Long.parseLong((String) uid);
        }
        throw new IllegalStateException("Token sem claim uid");
    }

    public boolean isTokenValid(String token, String expectedEmail) {
        try {
            String subject = extractEmail(token);
            Date exp = parseClaims(token).getExpiration();
            return subject.equalsIgnoreCase(expectedEmail) && exp.after(new Date());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret deve ter pelo menos 32 bytes (UTF-8)");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
