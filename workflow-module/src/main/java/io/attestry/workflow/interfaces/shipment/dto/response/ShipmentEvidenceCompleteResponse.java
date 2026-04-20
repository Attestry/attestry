package io.attestry.workflow.interfaces.shipment.dto.response;

import io.attestry.workflow.application.shipment.command.EvidenceCompleteResult;

public record ShipmentEvidenceCompleteResponse(
        String evidenceGroupId,
        String evidenceId,
        String status) {
    public static ShipmentEvidenceCompleteResponse from(EvidenceCompleteResult result) {
        return new ShipmentEvidenceCompleteResponse(result.evidenceGroupId(), result.evidenceId(), result.status());
    }
}
