package io.attestry.userauth.infrastructure.security;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAuthRuntimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
