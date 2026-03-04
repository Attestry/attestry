package io.attestry.workflow.domain.shipment.model;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Instant;

public record Shipment(
    String shipmentId,
    String tenantId,
    String groupId,
    String passportId,
    int shipmentRound,
    ShipmentStatus status,
    Instant releasedAt,
    String releasedByUserId,
    String releasedByGroupId,
    String evidenceGroupId,
    Instant returnedAt,
    String returnedByUserId,
    String returnEvidenceGroupId,
    Instant createdAt
) {

    public static Shipment release(
        String shipmentId,
        String tenantId,
        String groupId,
        String passportId,
        int shipmentRound,
        Instant releasedAt,
        String releasedByUserId,
        String releasedByGroupId,
        String evidenceGroupId,
        Instant createdAt
    ) {
        requireText(shipmentId, "shipmentId");
        requireText(tenantId, "tenantId");
        requireText(groupId, "groupId");
        requireText(passportId, "passportId");
        requireText(releasedByUserId, "releasedByUserId");
        requireText(releasedByGroupId, "releasedByGroupId");
        requireText(evidenceGroupId, "evidenceGroupId");
        if (shipmentRound <= 0) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "shipmentRound must be positive");
        }
        if (releasedAt == null || createdAt == null) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "releasedAt/createdAt are required");
        }
        return new Shipment(
            shipmentId,
            tenantId,
            groupId,
            passportId,
            shipmentRound,
            ShipmentStatus.RELEASED,
            releasedAt,
            releasedByUserId,
            releasedByGroupId,
            evidenceGroupId,
            null,
            null,
            null,
            createdAt
        );
    }

    public Shipment markReturned(String returnedByUserId, String returnEvidenceGroupId, Instant returnedAt) {
        requireText(returnedByUserId, "returnedByUserId");
        if (returnedAt == null) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "returnedAt is required");
        }
        if (status != ShipmentStatus.RELEASED) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Only RELEASED shipment can be returned");
        }
        return new Shipment(
            shipmentId,
            tenantId,
            groupId,
            passportId,
            shipmentRound,
            ShipmentStatus.RETURNED,
            releasedAt,
            releasedByUserId,
            releasedByGroupId,
            evidenceGroupId,
            returnedAt,
            returnedByUserId,
            returnEvidenceGroupId,
            createdAt
        );
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, field + " is required");
        }
    }
}
