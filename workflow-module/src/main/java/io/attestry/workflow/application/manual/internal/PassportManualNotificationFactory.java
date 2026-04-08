package io.attestry.workflow.application.manual.internal;

import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.domain.membership.model.NotificationType;
import io.attestry.userauth.domain.membership.model.PassportManualNotificationPayload;
import io.attestry.userauth.domain.membership.model.PassportManualNotificationPayload.AttachmentPayload;
import io.attestry.workflow.application.port.manual.PassportManualReadPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PassportManualNotificationFactory {

    private final Clock clock;

    public NotificationOutbox create(
        PassportManualReadPort.PassportManualContext context,
        String recipientEmail,
        String message,
        String evidenceGroupId,
        List<AttachmentPayload> attachments
    ) {
        return NotificationOutbox.create(
            NotificationType.PASSPORT_MANUAL_DELIVERY,
            recipientEmail,
            new PassportManualNotificationPayload(
                context.passportId(),
                context.tenantId(),
                recipientEmail,
                context.serialNumber(),
                context.modelName(),
                message,
                evidenceGroupId,
                attachments
            ),
            Instant.now(clock)
        );
    }
}
