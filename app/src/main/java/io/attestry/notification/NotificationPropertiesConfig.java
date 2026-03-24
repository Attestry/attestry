package io.attestry.notification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    InvitationNotificationProperties.class,
    PassportManualNotificationProperties.class,
    SignUpVerificationNotificationProperties.class
})
public class NotificationPropertiesConfig {
}
