package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import java.time.Instant;
import java.util.List;

public interface DistributionUseCase {

    BatchDistributeResult distribute(
        AuthPrincipal principal,
        String sourceTenantId,
        String partnerLinkId,
        DistributeCommand command
    );

    DistributionView recall(AuthPrincipal principal, String distributionId, RecallCommand command);

    PagedDistributionResponse listByTenant(AuthPrincipal principal, String sourceTenantId, int page, int size, String keyword);

    PagedDistributionCandidateResponse listDistributionCandidates(
        AuthPrincipal principal, int page, int size, String keyword
    );

    record RecallCommand(
        String reason
    ) {
    }

    record DistributeCommand(
        List<String> passportIds,
        Instant expiresAt,
        String note
    ) {
    }

    record BatchDistributeResult(
        List<Entry> results,
        int totalRequested,
        long totalDistributed
    ) {
        public record Entry(
            String passportId,
            String distributionId,
            String delegationId,
            String status,
            String error
        ) {
            public static Entry success(String passportId, String distributionId, String delegationId) {
                return new Entry(passportId, distributionId, delegationId, "DISTRIBUTED", null);
            }

            public static Entry failed(String passportId, String error) {
                return new Entry(passportId, null, null, "FAILED", error);
            }

            public boolean isSuccess() {
                return "DISTRIBUTED".equals(status);
            }
        }
    }

    record DistributionView(
        String distributionId,
        String passportId,
        String sourceTenantId,
        String targetTenantId,
        String targetTenantName,
        String targetTenantType,
        String partnerLinkId,
        String delegationId,
        String status,
        String serialNumber,
        String modelName,
        String distributedByUserId,
        Instant distributedAt,
        String recalledByUserId,
        Instant recalledAt,
        String recallReason
    ) {
    }

    record DistributionCandidateView(
        String passportId,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        String productionBatch,
        String factoryCode
    ) {
    }

    record PagedDistributionResponse(
        List<DistributionView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }

    record PagedDistributionCandidateResponse(
        List<DistributionCandidateView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}
