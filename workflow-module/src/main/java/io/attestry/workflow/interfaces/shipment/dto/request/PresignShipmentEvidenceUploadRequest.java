package io.attestry.workflow.interfaces.shipment.dto.request;

public record PresignShipmentEvidenceUploadRequest(
        String evidenceGroupId,
        String fileName,
        String contentType) {
}
