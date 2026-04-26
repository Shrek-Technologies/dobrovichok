package ru.dobrovichek.request.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RequestServiceConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
