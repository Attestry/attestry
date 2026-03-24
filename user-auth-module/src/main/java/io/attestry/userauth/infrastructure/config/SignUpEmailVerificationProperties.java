package io.attestry.userauth.infrastructure.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.user-auth.signup-email-verification")
public class SignUpEmailVerificationProperties {

    private Duration ttl = Duration.ofMinutes(10);
    private String fixedCode = "";
    private String hashSecret = "dev-signup-code-secret-change-me";
}
