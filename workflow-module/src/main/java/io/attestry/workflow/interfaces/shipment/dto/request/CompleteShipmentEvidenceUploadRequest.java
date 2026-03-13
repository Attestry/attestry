package io.attestry.workflow.interfaces.shipment.dto.request;

public record CompleteShipmentEvidenceUploadRequest(
        String evidenceGroupId,
        String evidenceId,
        long sizeBytes,
        String fileHash) {
}
