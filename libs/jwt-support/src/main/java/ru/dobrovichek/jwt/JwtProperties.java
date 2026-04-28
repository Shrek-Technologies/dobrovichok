package ru.dobrovichek.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties(prefix = "dobrovichek.jwt")
public class JwtProperties {

    private String secret = "dev-only-change-in-production-min-32-chars!!";

    private String issuer = "dobrovichek";

    private Duration accessTokenValidity = Duration.ofHours(24);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public void setAccessTokenValidity(Duration accessTokenValidity) {
        this.accessTokenValidity = accessTokenValidity;
    }

    public byte[] resolveSecretBytes() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("dobrovichek.jwt.secret must be at least 32 bytes (UTF-8)");
        }
        return bytes;
    }
}
