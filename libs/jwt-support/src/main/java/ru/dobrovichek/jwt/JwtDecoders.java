package ru.dobrovichek.jwt;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class JwtDecoders {

    private JwtDecoders() {
    }

    public static JwtDecoder nimbusMacSha256(JwtProperties properties) {
        SecretKey key = new SecretKeySpec(properties.resolveSecretBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    public static ReactiveJwtDecoder nimbusReactiveMacSha256(JwtProperties properties) {
        SecretKey key = new SecretKeySpec(properties.resolveSecretBytes(), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
