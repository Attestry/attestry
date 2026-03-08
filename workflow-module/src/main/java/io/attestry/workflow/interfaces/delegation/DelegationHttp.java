package io.attestry.workflow.interfaces.delegation;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.delegation.command.BatchGrantPassportDelegationCommand;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.BatchDelegationResult;
import io.attestry.workflow.application.delegation.result.DelegationEvaluateResult;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import io.attestry.workflow.application.usecase.DelegationLifecycleUseCase;
import io.attestry.workflow.application.usecase.DelegationUseCase;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflows")
public class DelegationHttp {

    private final DelegationUseCase delegationUseCase;
    private final DelegationLifecycleUseCase delegationLifecycleUseCase;

    public DelegationHttp(DelegationUseCase delegationUseCase, DelegationLifecycleUseCase delegationLifecycleUseCase) {
        this.delegationUseCase = delegationUseCase;
        this.delegationLifecycleUseCase = delegationLifecycleUseCase;
    }

    @PostMapping("/tenants/{sourceTenantId}/delegations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_GRANT')")
    public DelegationResponse grant(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("sourceTenantId") String sourceTenantId,
        @RequestBody GrantDelegationRequest request
    ) {
        DelegationResult result = delegationUseCase.grant(
            principal,
            sourceTenantId,
            new GrantDelegationCommand(
                request.partnerLinkId(),
                request.resourceType(),
                request.resourceId(),
                request.permissionCode(),
                request.expiresAt(),
                request.note()
            )
        );
        return DelegationResponse.from(result);
    }

    @PostMapping("/tenants/{sourceTenantId}/partners/{partnerLinkId}/passport-delegations")
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_GRANT')")
    public BatchDelegationResponse batchGrantPassportDelegation(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("sourceTenantId") String sourceTenantId,
        @PathVariable("partnerLinkId") String partnerLinkId,
        @RequestBody BatchGrantPassportDelegationRequest request
    ) {
        BatchDelegationResult result = delegationUseCase.batchGrantPassportDelegation(
            principal,
            sourceTenantId,
            partnerLinkId,
            new BatchGrantPassportDelegationCommand(
                request.passportIds(),
                request.expiresAt(),
                request.note()
            )
        );
        return BatchDelegationResponse.from(result);
    }

    @PostMapping("/delegations/{id}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_REVOKE')")
    public DelegationResponse revoke(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String delegationId,
        @RequestBody ReasonRequest request
    ) {
        return DelegationResponse.from(delegationUseCase.revoke(principal, delegationId, request.reason()));
    }

    @GetMapping("/tenants/{tenantId}/delegations")
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_READ')")
    public List<DelegationResponse> list(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId
    ) {
        return delegationUseCase.listByTenant(principal, tenantId).stream().map(DelegationResponse::from).toList();
    }

    @PostMapping("/internal/delegations/evaluate")
    public DelegationEvaluateResponse evaluate(@RequestBody DelegationEvaluateRequest request) {
        DelegationEvaluateResult result = delegationLifecycleUseCase.evaluate(
            request.resolvedSourceTenantId(),
            request.resolvedTargetTenantId(),
            request.resourceType(),
            request.resourceId(),
            request.permissionCode()
        );
        return new DelegationEvaluateResponse(result.allowed(), result.reason());
    }

    public record GrantDelegationRequest(
        String partnerLinkId,
        String resourceType,
        String resourceId,
        String permissionCode,
        Instant expiresAt,
        String note
    ) {
    }

    public record ReasonRequest(String reason) {
    }

    public record DelegationEvaluateRequest(
        String sourceTenantId,
        String targetTenantId,
        String brandTenantId,
        String partnerTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    ) {
        String resolvedSourceTenantId() {
            if (sourceTenantId != null && !sourceTenantId.isBlank()) {
                return sourceTenantId;
            }
            return brandTenantId;
        }

        String resolvedTargetTenantId() {
            if (targetTenantId != null && !targetTenantId.isBlank()) {
                return targetTenantId;
            }
            return partnerTenantId;
        }
    }

    public record DelegationEvaluateResponse(boolean allowed, String reason) {
    }

    public record BatchGrantPassportDelegationRequest(
        List<String> passportIds,
        Instant expiresAt,
        String note
    ) {
    }

    public record BatchDelegationResponse(
        List<BatchDelegationEntryResponse> results,
        int totalRequested,
        long totalGranted
    ) {
        static BatchDelegationResponse from(BatchDelegationResult result) {
            List<BatchDelegationEntryResponse> entries = result.results().stream()
                .map(e -> new BatchDelegationEntryResponse(
                    e.passportId(), e.delegationId(), e.status(), e.error()
                ))
                .toList();
            return new BatchDelegationResponse(entries, result.totalRequested(), result.totalGranted());
        }
    }

    public record BatchDelegationEntryResponse(
        String passportId,
        String delegationId,
        String status,
        String error
    ) {
    }

    public record DelegationResponse(
        String delegationId,
        String partnerLinkId,
        String sourceTenantId,
        String targetTenantId,
        String resourceType,
        String resourceId,
        String permissionCode,
        String status,
        Instant expiresAt,
        String reason
    ) {
        static DelegationResponse from(DelegationResult result) {
            return new DelegationResponse(
                result.delegationId(),
                result.partnerLinkId(),
                result.sourceTenantId(),
                result.targetTenantId(),
                result.resourceType(),
                result.resourceId(),
                result.permissionCode(),
                result.status(),
                result.expiresAt(),
                result.reason()
            );
        }
    }
}
