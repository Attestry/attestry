package io.attestry.workflow.interfaces.partner;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.partner.command.CreatePartnerLinkCommand;
import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import io.attestry.workflow.application.usecase.PartnerLinkUseCase;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.util.List;
import java.util.Locale;

import io.attestry.workflow.interfaces.partner.dto.request.CreatePartnerLinkRequest;
import io.attestry.workflow.interfaces.partner.dto.request.ReasonRequest;
import io.attestry.workflow.interfaces.partner.dto.response.PartnerLinkResponse;
import io.attestry.workflow.interfaces.partner.dto.response.TenantSearchResponse;
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

@RestController
@RequestMapping("/workflows")
public class PartnerLinkHttp {

    private final PartnerLinkUseCase partnerLinkUseCase;

    public PartnerLinkHttp(PartnerLinkUseCase partnerLinkUseCase) {
        this.partnerLinkUseCase = partnerLinkUseCase;
    }

    @PostMapping("/partner-links")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_CREATE')")
    public PartnerLinkResponse create(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody CreatePartnerLinkRequest request
    ) {
        PartnerLinkResult result = partnerLinkUseCase.create(
            principal,
            new CreatePartnerLinkCommand(
                request.targetTenantId(),
                request.partnerType(),
                request.proposedExpiresAt(),
                request.message()
            )
        );
        return PartnerLinkResponse.from(result);
    }

    @GetMapping("/partner-links")
    @PreAuthorize("hasAnyAuthority('SCOPE_TENANT_READ_ONLY')")
    public List<PartnerLinkResponse> list(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(name = "status", required = false) String status
    ) {
        PartnerLinkStatus statusFilter = parseStatus(status);
        return partnerLinkUseCase.listByTenant(principal, statusFilter).stream().map(PartnerLinkResponse::from).toList();
    }

    @GetMapping("/tenants/search")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_READ')")
    public List<TenantSearchResponse> searchActiveTenants(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam("name") String name
    ) {
        return partnerLinkUseCase.searchActiveTenantsByName(principal, name).stream()
            .map(TenantSearchResponse::from)
            .toList();
    }

    @PostMapping("/admin/partner-links/{id}/approve")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_APPROVE')")
    public PartnerLinkResponse approve(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String partnerLinkId
    ) {
        return PartnerLinkResponse.from(partnerLinkUseCase.approve(principal, partnerLinkId));
    }

    @PostMapping("/admin/partner-links/{id}/reject")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_APPROVE')")
    public PartnerLinkResponse reject(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String partnerLinkId,
        @RequestBody ReasonRequest request
    ) {
        return PartnerLinkResponse.from(partnerLinkUseCase.reject(principal, partnerLinkId, request.reason()));
    }

    @PostMapping("/partner-links/{id}/suspend")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_SUSPEND')")
    public PartnerLinkResponse suspend(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String partnerLinkId
    ) {
        return PartnerLinkResponse.from(partnerLinkUseCase.suspend(principal, partnerLinkId));
    }

    @PostMapping("/partner-links/{id}/resume")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_RESUME')")
    public PartnerLinkResponse resume(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String partnerLinkId
    ) {
        return PartnerLinkResponse.from(partnerLinkUseCase.resume(principal, partnerLinkId));
    }

    @PostMapping("/partner-links/{id}/terminate")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_TERMINATE')")
    public PartnerLinkResponse terminate(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String partnerLinkId,
        @RequestBody ReasonRequest request
    ) {
        return PartnerLinkResponse.from(partnerLinkUseCase.terminate(principal, partnerLinkId, request.reason()));
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


}
