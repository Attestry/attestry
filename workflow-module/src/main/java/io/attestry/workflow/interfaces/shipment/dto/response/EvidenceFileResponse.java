package io.attestry.workflow.interfaces.shipment.dto.response;

import io.attestry.workflow.application.shipment.result.ShipmentDetailResult;

public record EvidenceFileResponse(
        String evidenceId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String downloadUrl) {
    public static EvidenceFileResponse from(ShipmentDetailResult.EvidenceFileResult result) {
        return new EvidenceFileResponse(
                result.evidenceId(),
                result.originalFileName(),
                result.contentType(),
                result.sizeBytes(),
                result.downloadUrl());
    }
}
