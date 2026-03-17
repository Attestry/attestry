package io.attestry.notification;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.userauth.application.port.notification.PassportManualNotificationPort;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PassportManualAttachmentResolver {

    private static final Duration DOWNLOAD_URL_TTL = Duration.ofDays(7);

    private final ObjectStoragePort objectStoragePort;

    public PassportManualAttachmentResolver(
        ObjectStoragePort objectStoragePort
    ) {
        this.objectStoragePort = objectStoragePort;
    }

    public List<PassportManualNotificationPort.ManualAttachment> resolveAttachments(
        PassportManualNotificationPort.PassportManualNotification notification
    ) {
        if (notification.evidenceGroupId() == null || notification.evidenceGroupId().isBlank()) {
            return List.of();
        }

        return notification.attachmentFiles().stream()
            .map(attachment -> new PassportManualNotificationPort.ManualAttachment(
                attachment.evidenceId(),
                attachment.fileName(),
                objectStoragePort.issuePresignedDownload(attachment.objectKey(), DOWNLOAD_URL_TTL).downloadUrl()
            ))
            .toList();
    }
}
