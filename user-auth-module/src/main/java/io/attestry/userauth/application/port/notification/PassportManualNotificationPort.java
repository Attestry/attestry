package io.attestry.userauth.application.port.notification;

import java.util.List;

public interface PassportManualNotificationPort {

    void send(PassportManualNotification notification);

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
        List<String> attachmentEvidenceIds,
        List<ManualAttachment> attachments
    ) {
    }
}
