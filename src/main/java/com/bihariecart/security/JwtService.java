package com.bihariecart.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private static final String SECRET_KEY =
            "BihariECart@2026#JWT$SecurityKey!SpringBoot709106";

    // 1 minute clock skew tolerance to prevent false expiration from system clock micro-drifts
    private static final long CLOCK_SKEW_SECONDS = 60;

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(String email) {
        Instant now = Instant.now();
        Instant expiration = now.plus(1, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSignKey())
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}