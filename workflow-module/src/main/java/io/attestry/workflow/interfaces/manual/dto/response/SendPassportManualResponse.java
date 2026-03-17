package io.attestry.workflow.interfaces.manual.dto.response;

import io.attestry.workflow.application.manual.result.SendPassportManualResult;
import java.util.List;

public record SendPassportManualResponse(
    int queuedCount,
    boolean hasAttachment,
    String evidenceGroupId,
    List<PassportManualDeliveryResponse> deliveries
) {

    public static SendPassportManualResponse from(SendPassportManualResult result) {
        return new SendPassportManualResponse(
            result.queuedCount(),
            result.hasAttachment(),
            result.evidenceGroupId(),
            result.deliveries().stream()
                .map(delivery -> new PassportManualDeliveryResponse(
                    delivery.passportId(),
                    delivery.recipientEmailMasked()
                ))
                .toList()
        );
    }

    public record PassportManualDeliveryResponse(
        String passportId,
        String recipientEmailMasked
    ) {
    }
}
