package io.attestry.workflow.application.event;

import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.distribution.model.Distribution;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WorkflowEventPayloads {

    private WorkflowEventPayloads() {
    }

    public record DistributionCreatedPayload(
        String distributionId,
        String sourceTenantId,
        String targetTenantId,
        String partnerLinkId,
        String targetTenantName,
        String targetTenantType,
        String status,
        String distributedAt
    ) {
        public Map<String, Object> toMap() {
            return Map.of(
                "distributionId", distributionId,
                "sourceTenantId", sourceTenantId,
                "targetTenantId", targetTenantId,
                "partnerLinkId", partnerLinkId,
                "targetTenantName", targetTenantName,
                "targetTenantType", targetTenantType,
                "status", status,
                "distributedAt", distributedAt
            );
        }
    }

    public record DistributionRecalledPayload(
        String distributionId,
        String sourceTenantId,
        String targetTenantId,
        String reason,
        String targetTenantName,
        String targetTenantType,
        String status,
        String distributedAt
    ) {
        public Map<String, Object> toMap() {
            return Map.of(
                "distributionId", distributionId,
                "sourceTenantId", sourceTenantId,
                "targetTenantId", targetTenantId,
                "reason", reason,
                "targetTenantName", targetTenantName,
                "targetTenantType", targetTenantType,
                "status", status,
                "distributedAt", distributedAt
            );
        }
    }

    public record ShipmentReleasedPayload(
        String shipmentId,
        int shipmentRound,
        String tenantId,
        String evidenceGroupId,
        List<String> evidenceHashes,
        String status,
        String releasedAt,
        String releasedByUserDisplay,
        List<Map<String, Object>> evidenceDetails
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("shipmentId", shipmentId);
            map.put("shipmentRound", shipmentRound);
            map.put("tenantId", tenantId);
            map.put("evidenceGroupId", evidenceGroupId);
            map.put("evidenceHashes", evidenceHashes);
            map.put("status", status);
            map.put("releasedAt", releasedAt);
            map.put("releasedByUserDisplay", releasedByUserDisplay);
            map.put("evidenceDetails", evidenceDetails);
            return Map.copyOf(map);
        }
    }

    public record ShipmentReturnedPayload(
        String shipmentId,
        int shipmentRound,
        String tenantId,
        String returnEvidenceGroupId,
        List<String> returnEvidenceHashes,
        String reason,
        String status,
        String releasedAt,
        String releasedByUserDisplay,
        String returnedAt,
        String returnedByUserDisplay,
        List<Map<String, Object>> evidenceDetails
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("shipmentId", shipmentId);
            map.put("shipmentRound", shipmentRound);
            map.put("tenantId", tenantId);
            map.put("returnEvidenceGroupId", returnEvidenceGroupId);
            map.put("returnEvidenceHashes", returnEvidenceHashes);
            map.put("reason", reason);
            map.put("status", status);
            map.put("releasedAt", releasedAt);
            map.put("releasedByUserDisplay", releasedByUserDisplay);
            map.put("returnedAt", returnedAt);
            map.put("returnedByUserDisplay", returnedByUserDisplay);
            map.put("evidenceDetails", evidenceDetails);
            return Map.copyOf(map);
        }
    }

    public record OwnershipClaimedPayload(
        String transferId,
        String transferType,
        String toOwnerId,
        String tenantId,
        String completedAt
    ) {
        public Map<String, Object> toMap() {
            return Map.of(
                "transferId", transferId,
                "transferType", transferType,
                "toOwnerId", toOwnerId,
                "tenantId", tenantId,
                "completedAt", completedAt
            );
        }
    }

    public record OwnershipTransferCompletedPayload(
        String transferId,
        String transferType,
        String fromOwnerId,
        String toOwnerId
    ) {
        public Map<String, Object> toMap() {
            return Map.of(
                "transferId", transferId,
                "transferType", transferType,
                "fromOwnerId", fromOwnerId,
                "toOwnerId", toOwnerId
            );
        }
    }

    public record PurchaseClaimApprovedPayload(
        String claimId,
        String claimantUserId,
        String serialNumber,
        String modelName,
        String evidenceGroupId,
        List<String> evidenceHashes
    ) {
        public Map<String, Object> toMap() {
            return Map.of(
                "claimId", claimId,
                "claimantUserId", claimantUserId,
                "serialNumber", serialNumber,
                "modelName", modelName,
                "evidenceGroupId", evidenceGroupId,
                "evidenceHashes", evidenceHashes
            );
        }
    }

    public record ServiceConfirmedPayload(
        String serviceRequestId,
        String serviceType,
        String reason,
        String result,
        List<String> beforeEvidenceHashes,
        List<String> afterEvidenceHashes,
        String completedAt
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("serviceRequestId", serviceRequestId);
            map.put("serviceType", serviceType);
            map.put("reason", reason);
            map.put("result", result);
            map.put("beforeEvidenceHashes", beforeEvidenceHashes);
            map.put("afterEvidenceHashes", afterEvidenceHashes);
            map.put("completedAt", completedAt);
            return Map.copyOf(map);
        }
    }

    // --- factory methods ---

    public static DistributionCreatedPayload distributionCreatedPayload(
        Distribution distribution, String targetTenantName, String targetTenantType
    ) {
        return new DistributionCreatedPayload(
            distribution.distributionId(),
            distribution.sourceTenantId(),
            distribution.targetTenantId(),
            distribution.partnerLinkId(),
            nullToEmpty(targetTenantName),
            targetTenantType == null ? "UNKNOWN" : targetTenantType,
            distribution.status().name(),
            distribution.distributedAt().toString()
        );
    }

    public static DistributionRecalledPayload distributionRecalledPayload(
        Distribution distribution, String targetTenantName, String targetTenantType
    ) {
        return new DistributionRecalledPayload(
            distribution.distributionId(),
            distribution.sourceTenantId(),
            distribution.targetTenantId(),
            nullToEmpty(distribution.recallReason()),
            nullToEmpty(targetTenantName),
            targetTenantType == null ? "UNKNOWN" : targetTenantType,
            distribution.status().name(),
            distribution.distributedAt().toString()
        );
    }

    public static ShipmentReleasedPayload shipmentReleasedPayload(
        Shipment shipment, List<String> evidenceHashes,
        String releasedByUserDisplay, List<Map<String, Object>> evidences
    ) {
        return new ShipmentReleasedPayload(
            shipment.shipmentId(),
            shipment.shipmentRound(),
            shipment.tenantId(),
            shipment.evidenceGroupId(),
            evidenceHashes,
            shipment.status().name(),
            shipment.releasedAt().toString(),
            nullToEmpty(releasedByUserDisplay),
            evidences
        );
    }

    public static ShipmentReturnedPayload shipmentReturnedPayload(
        Shipment shipment, List<String> evidenceHashes, String reason,
        String releasedByUserDisplay, String returnedByUserDisplay,
        List<Map<String, Object>> evidences
    ) {
        return new ShipmentReturnedPayload(
            shipment.shipmentId(),
            shipment.shipmentRound(),
            shipment.tenantId(),
            nullToEmpty(shipment.returnEvidenceGroupId()),
            evidenceHashes,
            nullToEmpty(reason),
            shipment.status().name(),
            shipment.releasedAt().toString(),
            nullToEmpty(releasedByUserDisplay),
            shipment.returnedAt() == null ? "" : shipment.returnedAt().toString(),
            nullToEmpty(returnedByUserDisplay),
            evidences
        );
    }

    public static OwnershipClaimedPayload ownershipClaimedPayload(TokenTransfer transfer) {
        return new OwnershipClaimedPayload(
            transfer.transferId(),
            transfer.transferType().name(),
            transfer.toOwnerId(),
            nullToEmpty(transfer.tenantId()),
            transfer.completedAt() == null ? "" : transfer.completedAt().toString()
        );
    }

    public static OwnershipTransferCompletedPayload ownershipTransferCompletedPayload(TokenTransfer transfer) {
        return new OwnershipTransferCompletedPayload(
            transfer.transferId(),
            transfer.transferType().name(),
            transfer.fromOwnerId(),
            transfer.toOwnerId()
        );
    }

    public static PurchaseClaimApprovedPayload purchaseClaimApprovedPayload(
        PurchaseClaim claim, List<String> evidenceHashes
    ) {
        return new PurchaseClaimApprovedPayload(
            claim.claimId(),
            claim.claimantUserId(),
            claim.serialNumber(),
            claim.modelName(),
            claim.evidenceGroupId(),
            evidenceHashes
        );
    }

    public static ServiceConfirmedPayload serviceConfirmedPayload(
        ServiceRequest request, List<String> beforeEvidenceHashes,
        List<String> afterEvidenceHashes, String serviceResult
    ) {
        return new ServiceConfirmedPayload(
            request.serviceRequestId(),
            request.serviceType(),
            nullToEmpty(request.description()),
            nullToEmpty(serviceResult),
            beforeEvidenceHashes,
            afterEvidenceHashes,
            request.completedAt().toString()
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
