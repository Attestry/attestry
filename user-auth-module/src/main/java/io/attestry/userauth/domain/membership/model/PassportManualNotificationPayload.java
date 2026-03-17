package io.attestry.userauth.domain.membership.model;

import java.util.List;

public record PassportManualNotificationPayload(
    String passportId,
    String tenantId,
    String recipientEmail,
    String serialNumber,
    String modelName,
    String message,
    String evidenceGroupId,
    List<AttachmentPayload> attachments
) {
    public record AttachmentPayload(
        String evidenceId,
        String fileName,
        String objectKey,
        String contentType
    ) {
    }
}
