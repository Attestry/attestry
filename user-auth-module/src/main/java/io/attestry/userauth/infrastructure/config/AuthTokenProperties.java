package io.attestry.userauth.infrastructure.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth.token")
public class AuthTokenProperties {

    private Duration accessTtl = Duration.ofMinutes(15);
}
