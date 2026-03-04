package io.attestry.workflow.interfaces.partner;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.partner.command.CreatePartnerLinkCommand;
import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import io.attestry.workflow.application.usecase.PartnerLinkUseCase;
import java.util.List;

import io.attestry.workflow.interfaces.partner.dto.request.CreatePartnerLinkRequest;
import io.attestry.workflow.interfaces.partner.dto.request.ReasonRequest;
import io.attestry.workflow.interfaces.partner.dto.response.PartnerLinkResponse;
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
public class PartnerLinkHttp {

    private final PartnerLinkUseCase partnerLinkUseCase;

    public PartnerLinkHttp(PartnerLinkUseCase partnerLinkUseCase) {
        this.partnerLinkUseCase = partnerLinkUseCase;
    }

    @PostMapping("/tenants/{sourceTenantId}/partner-links")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_CREATE')")
    public PartnerLinkResponse create(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("sourceTenantId") String sourceTenantId,
        @RequestBody CreatePartnerLinkRequest request
    ) {
        PartnerLinkResult result = partnerLinkUseCase.create(
            principal,
            sourceTenantId,
            new CreatePartnerLinkCommand(
                request.resolvedTargetTenantId(),
                request.partnerType(),
                request.proposedExpiresAt(),
                request.message()
            )
        );
        return PartnerLinkResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/partner-links")
    @PreAuthorize("hasAuthority('SCOPE_PARTNER_LINK_READ')")
    public List<PartnerLinkResponse> list(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId
    ) {
        return partnerLinkUseCase.listByTenant(principal, tenantId).stream().map(PartnerLinkResponse::from).toList();
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


}
