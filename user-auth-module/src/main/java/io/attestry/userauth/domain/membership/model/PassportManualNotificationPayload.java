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
    List<String> attachmentEvidenceIds
) {
}
