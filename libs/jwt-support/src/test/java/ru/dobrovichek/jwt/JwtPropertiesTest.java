package ru.dobrovichek.jwt;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void resolveSecretBytesRequires32Utf8Bytes() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("short");
        assertThatThrownBy(properties::resolveSecretBytes)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void resolveSecretBytesAccepts32CharAscii() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-test-test-test-test-test-test-test-32bytes!");
        assertThat(properties.resolveSecretBytes()).hasSizeGreaterThanOrEqualTo(32);
    }

    @Test
    void defaults() {
        JwtProperties properties = new JwtProperties();
        assertThat(properties.getIssuer()).isEqualTo("dobrovichek");
        assertThat(properties.getAccessTokenValidity()).isEqualTo(Duration.ofHours(24));
    }
}
