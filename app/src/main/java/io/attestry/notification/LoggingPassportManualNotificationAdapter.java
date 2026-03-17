package io.attestry.notification;

import io.attestry.userauth.application.port.notification.PassportManualNotificationPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "app.workflow.passport-manual.mail",
    name = "provider",
    havingValue = "LOG",
    matchIfMissing = true
)
public class LoggingPassportManualNotificationAdapter implements PassportManualNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingPassportManualNotificationAdapter.class);

    private final boolean enabled;
    private final PassportManualAttachmentResolver attachmentResolver;

    public LoggingPassportManualNotificationAdapter(
        @Value("${app.workflow.passport-manual.mail.enabled:true}") boolean enabled,
        PassportManualAttachmentResolver attachmentResolver
    ) {
        this.enabled = enabled;
        this.attachmentResolver = attachmentResolver;
    }

    @Override
    public void send(PassportManualNotification notification) {
        if (!enabled) {
            return;
        }
        List<ManualAttachment> attachments = attachmentResolver.resolveAttachments(notification);
        log.info(
            "Passport manual email prepared. to={}, passportId={}, serialNumber={}, attachmentCount={}, message={}",
            notification.recipientEmail(),
            notification.passportId(),
            notification.serialNumber(),
            attachments.size(),
            notification.message()
        );
    }
}
