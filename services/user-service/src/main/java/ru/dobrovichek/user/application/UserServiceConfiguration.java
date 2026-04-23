package ru.dobrovichek.user.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UserServiceConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
