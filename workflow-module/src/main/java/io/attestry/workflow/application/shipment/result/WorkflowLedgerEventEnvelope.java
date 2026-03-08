package io.attestry.workflow.application.shipment.result;

import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferType;
import java.util.HashMap;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WorkflowLedgerEventEnvelope(
    String aggregateType,
    String passportId,
    String eventCategory,
    String eventAction,
    String actorRole,
    String actorId,
    Instant occurredAt,
    Map<String, Object> payload,
    String idempotencyKey
) {

    public static WorkflowLedgerEventEnvelope shipmentReleased(Shipment shipment, List<String> evidenceHashes) {
        return new WorkflowLedgerEventEnvelope(
            "SHIPMENT",
            shipment.passportId(),
            "SHIPMENT",
            "RELEASED",
            "BRAND",
            shipment.releasedByUserId(),
            shipment.releasedAt(),
            Map.of(
                "shipmentId", shipment.shipmentId(),
                "shipmentRound", shipment.shipmentRound(),
                "tenantId", shipment.tenantId(),
                "evidenceGroupId", shipment.evidenceGroupId(),
                "evidenceHashes", evidenceHashes
            ),
            "shipment-release-" + shipment.shipmentId()
        );
    }

    public static WorkflowLedgerEventEnvelope shipmentReturned(Shipment shipment, List<String> evidenceHashes, String reason) {
        return new WorkflowLedgerEventEnvelope(
            "SHIPMENT",
            shipment.passportId(),
            "SHIPMENT",
            "RETURNED",
            "BRAND",
            shipment.returnedByUserId(),
            shipment.returnedAt(),
            Map.of(
                "shipmentId", shipment.shipmentId(),
                "shipmentRound", shipment.shipmentRound(),
                "tenantId", shipment.tenantId(),
                "returnEvidenceGroupId", shipment.returnEvidenceGroupId() == null ? "" : shipment.returnEvidenceGroupId(),
                "returnEvidenceHashes", evidenceHashes,
                "reason", reason == null ? "" : reason
            ),
            "shipment-return-" + shipment.shipmentId()
        );
    }

    public static WorkflowLedgerEventEnvelope ownershipClaimed(TokenTransfer transfer) {
        return new WorkflowLedgerEventEnvelope(
            "TRANSFER",
            transfer.passportId(),
            "OWNERSHIP",
            "CLAIMED",
            "RETAIL",
            transfer.createdByUserId(),
            transfer.completedAt(),
            Map.of(
                "transferId", transfer.transferId(),
                "transferType", transfer.transferType().name(),
                "toOwnerId", transfer.toOwnerId(),
                "tenantId", transfer.tenantId() == null ? "" : transfer.tenantId()
            ),
            "transfer-claimed-" + transfer.transferId()
        );
    }

    public static WorkflowLedgerEventEnvelope ownershipTransferCompleted(TokenTransfer transfer) {
        return new WorkflowLedgerEventEnvelope(
            "TRANSFER",
            transfer.passportId(),
            "OWNERSHIP",
            "TRANSFER_COMPLETED",
            "OWNER",
            transfer.fromOwnerId(),
            transfer.completedAt(),
            Map.of(
                "transferId", transfer.transferId(),
                "transferType", transfer.transferType().name(),
                "fromOwnerId", transfer.fromOwnerId(),
                "toOwnerId", transfer.toOwnerId()
            ),
            "transfer-completed-" + transfer.transferId()
        );
    }

    public static WorkflowLedgerEventEnvelope purchaseClaimApproved(
        PurchaseClaim claim,
        List<String> evidenceHashes,
        String actorRole,
        String actorId
    ) {
        return new WorkflowLedgerEventEnvelope(
            "PURCHASE_CLAIM",
            claim.passportId(),
            "OWNERSHIP",
            "PURCHASE_CLAIM_APPROVED",
            actorRole,
            actorId,
            claim.reviewedAt(),
            Map.of(
                "claimId", claim.claimId(),
                "claimantUserId", claim.claimantUserId(),
                "serialNumber", claim.serialNumber(),
                "modelName", claim.modelName(),
                "evidenceGroupId", claim.evidenceGroupId(),
                "evidenceHashes", evidenceHashes
            ),
            "purchase-claim-approved-" + claim.claimId()
        );
    }

    public static WorkflowLedgerEventEnvelope serviceConfirmed(
        ServiceRequest request,
        List<String> beforeEvidenceHashes,
        List<String> afterEvidenceHashes,
        String serviceResult
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("serviceRequestId", request.serviceRequestId());
        payload.put("serviceType", request.serviceType());
        payload.put("reason", request.description() == null ? "" : request.description());
        payload.put("result", serviceResult == null ? "" : serviceResult);
        payload.put("beforeEvidenceHashes", beforeEvidenceHashes);
        payload.put("afterEvidenceHashes", afterEvidenceHashes);
        payload.put("completedAt", request.completedAt().toString());

        return new WorkflowLedgerEventEnvelope(
            "SERVICE_REQUEST",
            request.passportId(),
            "SERVICE",
            "SERVICE_CONFIRMED",
            "SERVICE_PROVIDER",
            request.completedByUserId(),
            request.completedAt(),
            Map.copyOf(payload),
            "svc-complete-" + request.serviceRequestId()
        );
    }
}
