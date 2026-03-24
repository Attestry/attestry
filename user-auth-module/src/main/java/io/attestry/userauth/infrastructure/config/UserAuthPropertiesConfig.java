package io.attestry.userauth.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    AuthTokenProperties.class,
    SignUpEmailVerificationProperties.class
})
public class UserAuthPropertiesConfig {
}
