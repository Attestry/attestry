package io.attestry.workflow.domain.event;

import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WorkflowLedgerEvents {

    private WorkflowLedgerEvents() {
    }

    public static OutboxEventEnvelope shipmentReleased(Shipment shipment, List<String> evidenceHashes) {
        return new OutboxEventEnvelope(
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

    public static OutboxEventEnvelope shipmentReturned(Shipment shipment, List<String> evidenceHashes, String reason) {
        return new OutboxEventEnvelope(
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

    public static OutboxEventEnvelope ownershipClaimed(TokenTransfer transfer) {
        return new OutboxEventEnvelope(
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

    public static OutboxEventEnvelope ownershipTransferCompleted(TokenTransfer transfer) {
        return new OutboxEventEnvelope(
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

    public static OutboxEventEnvelope purchaseClaimApproved(
        PurchaseClaim claim,
        List<String> evidenceHashes,
        String actorRole,
        String actorId
    ) {
        return new OutboxEventEnvelope(
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

    public static OutboxEventEnvelope serviceConfirmed(
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

        return new OutboxEventEnvelope(
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
