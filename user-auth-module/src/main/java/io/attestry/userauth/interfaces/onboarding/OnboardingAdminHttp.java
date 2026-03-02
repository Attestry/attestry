package io.attestry.userauth.interfaces.onboarding;

import io.attestry.userauth.application.dto.result.ApproveApplicationResult;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.usecase.onboarding.OnboardingUseCase;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.interfaces.onboarding.dto.request.RejectApplicationRequest;
import io.attestry.userauth.interfaces.onboarding.dto.response.ApplicationResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.ApproveResponse;
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
@RequestMapping
public class OnboardingAdminHttp {

    private final OnboardingUseCase onboardingService;

    public OnboardingAdminHttp(OnboardingUseCase onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/admin/brand-applications")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public List<ApplicationResponse> listBrandApplications() {
        return onboardingService.listBrandApplications().stream().map(ApplicationResponse::from).toList();
    }

    @PostMapping("/admin/brand-applications/{applicationId}/approve")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public ApproveResponse approveBrandApplication(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable(name = "applicationId") String applicationId
    ) {
        ApproveApplicationResult result = onboardingService.approveBrandApplication(toActorContext(principal), applicationId);
        return new ApproveResponse(result.tenantId(), result.groupId(), result.membershipId());
    }

    @PostMapping("/admin/brand-applications/{applicationId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public void rejectBrandApplication(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable(name = "applicationId") String applicationId,
        @RequestBody RejectApplicationRequest request
    ) {
        onboardingService.rejectBrandApplication(toActorContext(principal), applicationId, request.reason());
    }

    @GetMapping("/admin/retail-applications")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public List<ApplicationResponse> listRetailApplications() {
        return onboardingService.listRetailApplications().stream().map(ApplicationResponse::from).toList();
    }

    @PostMapping("/admin/retail-applications/{id}/approve")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public ApproveResponse approveRetailApplication(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String id
    ) {
        ApproveApplicationResult result = onboardingService.approveRetailApplication(toActorContext(principal), id);
        return new ApproveResponse(result.tenantId(), result.groupId(), result.membershipId());
    }

    @PostMapping("/admin/retail-applications/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public void rejectRetailApplication(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("id") String id,
        @RequestBody RejectApplicationRequest request
    ) {
        onboardingService.rejectRetailApplication(toActorContext(principal), id, request.reason());
    }

    private ActorContext toActorContext(AuthPrincipal principal) {
        return new ActorContext(
            principal.tokenId(),
            principal.userId(),
            principal.tenantId(),
            principal.groupId(),
            principal.verificationLevel(),
            principal.scopes(),
            principal.expiresAt()
        );
    }
}
