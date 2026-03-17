package io.attestry.userauth.application.port.notification;

import java.util.List;

public interface PassportManualNotificationPort {

    void send(PassportManualNotification notification);

    record AttachmentReference(
        String evidenceId,
        String fileName,
        String objectKey,
        String contentType
    ) {
    }

    record ManualAttachment(
        String evidenceId,
        String fileName,
        String downloadUrl
    ) {
    }

    record PassportManualNotification(
        String passportId,
        String recipientEmail,
        String serialNumber,
        String modelName,
        String message,
        String evidenceGroupId,
        List<AttachmentReference> attachmentFiles,
        List<ManualAttachment> attachments
    ) {
    }
}
