package io.attestry.notification.logging;

import io.attestry.notification.SignUpVerificationNotificationProperties;
import io.attestry.userauth.application.port.notification.SignUpEmailVerificationNotificationPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.user-auth.signup-email-verification.mail",
    name = "provider",
    havingValue = "LOG",
    matchIfMissing = true
)
public class LoggingSignUpEmailVerificationNotificationAdapter implements SignUpEmailVerificationNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingSignUpEmailVerificationNotificationAdapter.class);

    private final SignUpVerificationNotificationProperties properties;

    @Override
    public void send(SignUpEmailVerificationNotification notification) {
        if (!properties.getMail().isEnabled()) {
            return;
        }
        log.info(
            "Signup email verification prepared. to={}, verificationId={}, dedupeKey={}, code={}, expiresInSeconds={}",
            notification.email(),
            notification.verificationId(),
            notification.dedupeKey(),
            notification.code(),
            notification.expiresInSeconds()
        );
    }
}
