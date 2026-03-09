package io.attestry.workflow.interfaces.distribution;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.usecase.DistributionUseCase;
import io.attestry.workflow.application.usecase.DistributionUseCase.BatchDistributeResult;
import io.attestry.workflow.application.usecase.DistributionUseCase.DistributeCommand;
import io.attestry.workflow.application.usecase.DistributionUseCase.DistributionCandidateView;
import io.attestry.workflow.application.usecase.DistributionUseCase.DistributionView;
import io.attestry.workflow.application.usecase.DistributionUseCase.PagedDistributionCandidateResponse;
import io.attestry.workflow.application.usecase.DistributionUseCase.PagedDistributionResponse;
import io.attestry.workflow.application.usecase.DistributionUseCase.RecallCommand;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflows")
public class DistributionHttp {

    private final DistributionUseCase distributionUseCase;

    public DistributionHttp(DistributionUseCase distributionUseCase) {
        this.distributionUseCase = distributionUseCase;
    }

    @PostMapping("/tenants/{sourceTenantId}/partners/{partnerLinkId}/distributions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_GRANT')")
    public BatchDistributionResponse distribute(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("sourceTenantId") String sourceTenantId,
        @PathVariable("partnerLinkId") String partnerLinkId,
        @RequestBody DistributeRequest request
    ) {
        BatchDistributeResult result = distributionUseCase.distribute(
            principal,
            sourceTenantId,
            partnerLinkId,
            new DistributeCommand(
                request.passportIds(),
                request.expiresAt(),
                request.note()
            )
        );
        return BatchDistributionResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/distributions")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public PagedDistributionResponse list(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return distributionUseCase.listByTenant(tenantId, page, size, keyword);
    }

    @GetMapping("/distributions/candidates")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public PagedDistributionCandidateResponse listCandidates(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return distributionUseCase.listDistributionCandidates(principal, page, size, keyword);
    }

    @PostMapping("/distributions/{distributionId}/recall")
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_GRANT')")
    public DistributionResponse recall(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("distributionId") String distributionId,
        @RequestBody RecallRequest request
    ) {
        DistributionView result = distributionUseCase.recall(
            principal, distributionId, new RecallCommand(request.reason())
        );
        return DistributionResponse.from(result);
    }

    public record RecallRequest(
        String reason
    ) {
    }

    public record DistributeRequest(
        List<String> passportIds,
        Instant expiresAt,
        String note
    ) {
    }

    public record DistributionResponse(
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
        static DistributionResponse from(DistributionView view) {
            return new DistributionResponse(
                view.distributionId(),
                view.passportId(),
                view.sourceTenantId(),
                view.targetTenantId(),
                view.targetTenantName(),
                view.targetTenantType(),
                view.partnerLinkId(),
                view.delegationId(),
                view.status(),
                view.serialNumber(),
                view.modelName(),
                view.distributedByUserId(),
                view.distributedAt(),
                view.recalledByUserId(),
                view.recalledAt(),
                view.recallReason()
            );
        }
    }

    public record BatchDistributionResponse(
        List<BatchDistributionEntryResponse> results,
        int totalRequested,
        long totalDistributed
    ) {
        static BatchDistributionResponse from(BatchDistributeResult result) {
            List<BatchDistributionEntryResponse> entries = result.results().stream()
                .map(e -> new BatchDistributionEntryResponse(
                    e.passportId(), e.distributionId(), e.delegationId(), e.status(), e.error()
                ))
                .toList();
            return new BatchDistributionResponse(entries, result.totalRequested(), result.totalDistributed());
        }
    }

    public record BatchDistributionEntryResponse(
        String passportId,
        String distributionId,
        String delegationId,
        String status,
        String error
    ) {
    }
}
