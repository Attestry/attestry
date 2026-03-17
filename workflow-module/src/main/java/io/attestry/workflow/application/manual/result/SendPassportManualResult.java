package io.attestry.workflow.application.manual.result;

import java.util.List;

public record SendPassportManualResult(
    int queuedCount,
    boolean hasAttachment,
    String evidenceGroupId,
    List<PassportManualDeliveryResult> deliveries
) {
    public record PassportManualDeliveryResult(
        String passportId,
        String recipientEmailMasked
    ) {
    }
}
