package ru.dobrovichek.identity.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.dobrovichek.jwt.JwtProperties;
import ru.dobrovichek.jwt.JwtTokenIssuer;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtIssuingConfiguration {

    @Bean
    JwtTokenIssuer jwtTokenIssuer(JwtProperties properties) {
        return new JwtTokenIssuer(properties);
    }
}
