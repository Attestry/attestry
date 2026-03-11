package io.attestry.workflow.interfaces.distribution;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.usecase.DistributionUseCase;
import io.attestry.workflow.application.usecase.DistributionUseCase.BatchDistributeResult;
import io.attestry.workflow.application.usecase.DistributionUseCase.DistributeCommand;
import io.attestry.workflow.application.usecase.DistributionUseCase.DistributionView;
import io.attestry.workflow.application.usecase.DistributionUseCase.PagedDistributionCandidateResponse;
import io.attestry.workflow.application.usecase.DistributionUseCase.PagedDistributionResponse;
import io.attestry.workflow.application.usecase.DistributionUseCase.RecallCommand;
import io.attestry.workflow.interfaces.distribution.dto.request.DistributeRequest;
import io.attestry.workflow.interfaces.distribution.dto.request.RecallRequest;
import io.attestry.workflow.interfaces.distribution.dto.response.BatchDistributionResponse;
import io.attestry.workflow.interfaces.distribution.dto.response.DistributionResponse;
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
    public ApiResponse<BatchDistributionResponse> distribute(
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
        return ApiResponse.success(BatchDistributionResponse.from(result));
    }

    @GetMapping("/tenants/{tenantId}/distributions")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<PagedDistributionResponse> list(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(distributionUseCase.listByTenant(tenantId, page, size, keyword));
    }

    @GetMapping("/distributions/candidates")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<PagedDistributionCandidateResponse> listCandidates(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(distributionUseCase.listDistributionCandidates(principal, page, size, keyword));
    }

    @PostMapping("/distributions/{distributionId}/recall")
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_GRANT')")
    public ApiResponse<DistributionResponse> recall(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("distributionId") String distributionId,
        @RequestBody RecallRequest request
    ) {
        DistributionView result = distributionUseCase.recall(
            principal, distributionId, new RecallCommand(request.reason())
        );
        return ApiResponse.success(DistributionResponse.from(result));
    }
}
