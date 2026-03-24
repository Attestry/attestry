package io.attestry.workflow.interfaces.delegation;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.DelegationEvaluateResult;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import io.attestry.workflow.application.delegation.usecase.DelegationLifecycleUseCase;
import io.attestry.workflow.application.delegation.usecase.DelegationUseCase;
import io.attestry.workflow.interfaces.delegation.dto.request.DelegationEvaluateRequest;
import io.attestry.workflow.interfaces.delegation.dto.request.GrantDelegationRequest;
import io.attestry.workflow.interfaces.delegation.dto.request.ReasonRequest;
import io.attestry.workflow.interfaces.delegation.dto.response.DelegationEvaluateResponse;
import io.attestry.workflow.interfaces.delegation.dto.response.DelegationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/workflows")
public class DelegationHttp {

    private final DelegationUseCase delegationUseCase;
    private final DelegationLifecycleUseCase delegationLifecycleUseCase;


    @PostMapping("/tenants/{sourceTenantId}/delegations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_GRANT')")
    public ApiResponse<DelegationResponse> grant(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("sourceTenantId") String sourceTenantId,
        @RequestBody GrantDelegationRequest request
    ) {
        DelegationResult result = delegationUseCase.grant(
            actor(principal),
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
        return ApiResponse.success(DelegationResponse.from(result));
    }

    @PostMapping("/delegations/{id}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_DELEGATION_REVOKE')")
    public ApiResponse<DelegationResponse> revoke(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String delegationId,
        @RequestBody ReasonRequest request
    ) {
        return ApiResponse.success(DelegationResponse.from(delegationUseCase.revoke(actor(principal), delegationId, request.reason())));
    }

    @PostMapping("/internal/delegations/evaluate")
    public ApiResponse<DelegationEvaluateResponse> evaluate(@RequestBody DelegationEvaluateRequest request) {
        DelegationEvaluateResult result = delegationLifecycleUseCase.evaluate(
            request.resolvedSourceTenantId(),
            request.resolvedTargetTenantId(),
            request.resourceType(),
            request.resourceId(),
            request.permissionCode()
        );
        return ApiResponse.success(new DelegationEvaluateResponse(result.allowed(), result.reason()));
    }

    private WorkflowActorContext actor(AuthPrincipal principal) {
        return WorkflowActorContext.from(principal);
    }
}
