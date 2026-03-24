package io.attestry.workflow.interfaces.partner;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.partner.command.CreatePartnerLinkCommand;
import io.attestry.workflow.application.partner.usecase.PartnerLinkCommandUseCase;
import io.attestry.workflow.application.partner.usecase.PartnerLinkQueryUseCase;
import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.util.List;
import java.util.Locale;

import io.attestry.workflow.interfaces.partner.dto.request.CreatePartnerLinkRequest;
import io.attestry.workflow.interfaces.partner.dto.request.ReasonRequest;
import io.attestry.workflow.interfaces.partner.dto.response.PartnerLinkResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@RestController
@RequestMapping("/workflows")
public class PartnerLinkHttp {

    private final PartnerLinkCommandUseCase partnerLinkCommandUseCase;
    private final PartnerLinkQueryUseCase partnerLinkQueryUseCase;


    @PostMapping("/partner-links")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_CREATE')")
    public ApiResponse<PartnerLinkResponse> create(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody CreatePartnerLinkRequest request
    ) {
        PartnerLinkResult result = partnerLinkCommandUseCase.create(
            actor(principal),
            new CreatePartnerLinkCommand(
                request.targetTenantId(),
                request.partnerType(),
                request.proposedExpiresAt(),
                request.message()
            )
        );
        return ApiResponse.success(PartnerLinkResponse.from(result));
    }

    @GetMapping("/partner-links")
    @PreAuthorize("hasAnyAuthority('SCOPE_TENANT_READ_ONLY')")
    public ApiResponse<List<PartnerLinkResponse>> list(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(name = "status", required = false) String status
    ) {
        PartnerLinkStatus statusFilter = parseStatus(status);
        return ApiResponse.success(partnerLinkQueryUseCase.listByTenant(actor(principal), statusFilter).stream().map(PartnerLinkResponse::from).toList());
    }

    @PostMapping("/admin/partner-links/{id}/approve")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_APPROVE')")
    public ApiResponse<PartnerLinkResponse> approve(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String partnerLinkId
    ) {
        return ApiResponse.success(PartnerLinkResponse.from(partnerLinkCommandUseCase.approve(actor(principal), partnerLinkId)));
    }

    @PostMapping("/admin/partner-links/{id}/reject")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_APPROVE')")
    public ApiResponse<PartnerLinkResponse> reject(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String partnerLinkId,
        @Valid @RequestBody ReasonRequest request
    ) {
        return ApiResponse.success(PartnerLinkResponse.from(partnerLinkCommandUseCase.reject(actor(principal), partnerLinkId, request.reason())));
    }

    @PostMapping("/partner-links/{id}/suspend")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_SUSPEND')")
    public ApiResponse<PartnerLinkResponse> suspend(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String partnerLinkId
    ) {
        return ApiResponse.success(PartnerLinkResponse.from(partnerLinkCommandUseCase.suspend(actor(principal), partnerLinkId)));
    }

    @PostMapping("/partner-links/{id}/resume")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_RESUME')")
    public ApiResponse<PartnerLinkResponse> resume(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String partnerLinkId
    ) {
        return ApiResponse.success(PartnerLinkResponse.from(partnerLinkCommandUseCase.resume(actor(principal), partnerLinkId)));
    }

    @PostMapping("/partner-links/{id}/terminate")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_TERMINATE')")
    public ApiResponse<PartnerLinkResponse> terminate(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String partnerLinkId,
        @Valid @RequestBody ReasonRequest request
    ) {
        return ApiResponse.success(PartnerLinkResponse.from(partnerLinkCommandUseCase.terminate(actor(principal), partnerLinkId, request.reason())));
    }

    private PartnerLinkStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PartnerLinkStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Invalid status filter: " + status);
        }
    }

    private WorkflowActorContext actor(AuthPrincipal principal) {
        return WorkflowActorContext.from(principal);
    }

}
