package ru.dobrovichek.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ru.dobrovichek.contracts.UserRole;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class JwtTokenIssuer {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenIssuer(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.resolveSecretBytes());
    }

    public String createAccessToken(UUID userId, UserRole role) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getAccessTokenValidity());
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(userId.toString())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
