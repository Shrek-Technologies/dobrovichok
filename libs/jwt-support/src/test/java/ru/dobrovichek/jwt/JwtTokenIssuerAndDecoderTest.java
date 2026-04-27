package ru.dobrovichek.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import ru.dobrovichek.contracts.UserRole;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenIssuerAndDecoderTest {

    private JwtProperties properties;
    private JwtTokenIssuer issuer;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("test-test-test-test-test-test-test-test-32bytes!");
        properties.setIssuer("test-issuer");
        issuer = new JwtTokenIssuer(properties);
    }

    @Test
    void servletDecoderValidatesIssuerAndClaims() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String compact = issuer.createAccessToken(userId, UserRole.WARD);

        JwtDecoder decoder = JwtDecoders.nimbusMacSha256(properties);
        Jwt jwt = decoder.decode(compact);

        assertThat(jwt.getClaimAsString("iss")).isEqualTo("test-issuer");
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("WARD");
    }

    @Test
    void reactiveDecoderValidatesToken() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String compact = issuer.createAccessToken(userId, UserRole.VOLUNTEER);

        ReactiveJwtDecoder decoder = JwtDecoders.nimbusReactiveMacSha256(properties);
        Jwt jwt = decoder.decode(compact).block();

        assertThat(jwt).isNotNull();
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("VOLUNTEER");
    }
}
