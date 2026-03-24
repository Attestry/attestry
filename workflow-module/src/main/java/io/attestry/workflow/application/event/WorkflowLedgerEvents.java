package io.attestry.workflow.application.event;

import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.distribution.model.Distribution;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferType;
import java.util.List;
import java.util.Map;

public final class WorkflowLedgerEvents {

    private WorkflowLedgerEvents() {
    }

    public static OutboxEventEnvelope distributionCreated(
        Distribution distribution, String targetTenantName, String targetTenantType
    ) {
        return new OutboxEventEnvelope(
            "DISTRIBUTION",
            distribution.passportId(),
            "DISTRIBUTION",
            "CREATED",
            "BRAND",
            distribution.distributedByUserId(),
            distribution.distributedAt(),
            WorkflowEventPayloads.distributionCreatedPayload(distribution, targetTenantName, targetTenantType).toMap(),
            "distribution-created-" + distribution.distributionId()
        );
    }

    public static OutboxEventEnvelope distributionRecalled(
        Distribution distribution, String targetTenantName, String targetTenantType
    ) {
        return new OutboxEventEnvelope(
            "DISTRIBUTION",
            distribution.passportId(),
            "DISTRIBUTION",
            "RECALLED",
            "BRAND",
            distribution.recalledByUserId(),
            distribution.recalledAt(),
            WorkflowEventPayloads.distributionRecalledPayload(distribution, targetTenantName, targetTenantType).toMap(),
            "distribution-recalled-" + distribution.distributionId()
        );
    }

    public static OutboxEventEnvelope shipmentReleased(
        Shipment shipment, List<String> evidenceHashes,
        String releasedByUserDisplay, List<Map<String, Object>> evidences
    ) {
        return new OutboxEventEnvelope(
            "SHIPMENT",
            shipment.passportId(),
            "SHIPMENT",
            "RELEASED",
            "BRAND",
            shipment.releasedByUserId(),
            shipment.releasedAt(),
            WorkflowEventPayloads.shipmentReleasedPayload(shipment, evidenceHashes, releasedByUserDisplay, evidences).toMap(),
            "shipment-release-" + shipment.shipmentId()
        );
    }

    public static OutboxEventEnvelope shipmentReturned(
        Shipment shipment, List<String> evidenceHashes, String reason,
        String releasedByUserDisplay, String returnedByUserDisplay,
        List<Map<String, Object>> evidences
    ) {
        return new OutboxEventEnvelope(
            "SHIPMENT",
            shipment.passportId(),
            "SHIPMENT",
            "RETURNED",
            "BRAND",
            shipment.returnedByUserId(),
            shipment.returnedAt(),
            WorkflowEventPayloads.shipmentReturnedPayload(shipment, evidenceHashes, reason, releasedByUserDisplay, returnedByUserDisplay, evidences).toMap(),
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
            WorkflowEventPayloads.ownershipClaimedPayload(transfer).toMap(),
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
            WorkflowEventPayloads.ownershipTransferCompletedPayload(transfer).toMap(),
            "transfer-completed-" + transfer.transferId()
        );
    }

    public static OutboxEventEnvelope purchaseClaimApproved(
        PurchaseClaim claim, List<String> evidenceHashes,
        String actorRole, String actorId
    ) {
        return new OutboxEventEnvelope(
            "PURCHASE_CLAIM",
            claim.passportId(),
            "OWNERSHIP",
            "PURCHASE_CLAIM_APPROVED",
            actorRole,
            actorId,
            claim.reviewedAt(),
            WorkflowEventPayloads.purchaseClaimApprovedPayload(claim, evidenceHashes).toMap(),
            "purchase-claim-approved-" + claim.claimId()
        );
    }

    public static OutboxEventEnvelope serviceConfirmed(
        ServiceRequest request, List<String> beforeEvidenceHashes,
        List<String> afterEvidenceHashes, String serviceResult
    ) {
        return new OutboxEventEnvelope(
            "SERVICE_REQUEST",
            request.passportId(),
            "SERVICE",
            "SERVICE_CONFIRMED",
            "SERVICE_PROVIDER",
            request.completedByUserId(),
            request.completedAt(),
            WorkflowEventPayloads.serviceConfirmedPayload(request, beforeEvidenceHashes, afterEvidenceHashes, serviceResult).toMap(),
            "svc-complete-" + request.serviceRequestId()
        );
    }
}
