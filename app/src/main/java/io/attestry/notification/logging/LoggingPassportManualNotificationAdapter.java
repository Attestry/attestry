package io.attestry.notification.logging;

import io.attestry.notification.PassportManualAttachmentResolver;
import io.attestry.notification.PassportManualNotificationProperties;
import io.attestry.userauth.application.port.notification.PassportManualNotificationPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.workflow.passport-manual.mail",
    name = "provider",
    havingValue = "LOG",
    matchIfMissing = true
)
public class LoggingPassportManualNotificationAdapter implements PassportManualNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingPassportManualNotificationAdapter.class);

    private final PassportManualNotificationProperties properties;
    private final PassportManualAttachmentResolver attachmentResolver;

    @Override
    public void send(PassportManualNotification notification) {
        if (!properties.getMail().isEnabled()) {
            return;
        }
        List<ManualAttachment> attachments = attachmentResolver.resolveAttachments(notification);
        log.info(
            "Passport manual email prepared. to={}, passportId={}, dedupeKey={}, serialNumber={}, attachmentCount={}, message={}",
            notification.recipientEmail(),
            notification.passportId(),
            notification.dedupeKey(),
            notification.serialNumber(),
            attachments.size(),
            notification.message()
        );
    }
}
