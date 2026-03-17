package io.attestry.notification;

import io.attestry.userauth.application.port.notification.SignUpEmailVerificationNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "app.user-auth.signup-email-verification.mail",
    name = "provider",
    havingValue = "LOG",
    matchIfMissing = true
)
public class LoggingSignUpEmailVerificationNotificationAdapter implements SignUpEmailVerificationNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingSignUpEmailVerificationNotificationAdapter.class);

    private final boolean enabled;

    public LoggingSignUpEmailVerificationNotificationAdapter(
        @Value("${app.user-auth.signup-email-verification.mail.enabled:true}") boolean enabled
    ) {
        this.enabled = enabled;
    }

    @Override
    public void send(SignUpEmailVerificationNotification notification) {
        if (!enabled) {
            return;
        }
        log.info(
            "Signup email verification prepared. to={}, verificationId={}, code={}, expiresInSeconds={}",
            notification.email(),
            notification.verificationId(),
            notification.code(),
            notification.expiresInSeconds()
        );
    }
}
