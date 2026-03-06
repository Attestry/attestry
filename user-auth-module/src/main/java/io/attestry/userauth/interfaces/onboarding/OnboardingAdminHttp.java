package io.attestry.userauth.interfaces.onboarding;

import io.attestry.userauth.application.dto.result.ApproveApplicationResult;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.usecase.onboarding.OnboardingUseCase;
import io.attestry.userauth.security.CurrentActor;
import io.attestry.userauth.interfaces.onboarding.dto.request.RejectApplicationRequest;
import io.attestry.userauth.interfaces.onboarding.dto.response.ApplicationResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.ApproveResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class OnboardingAdminHttp {

    private final OnboardingUseCase onboardingService;

    public OnboardingAdminHttp(OnboardingUseCase onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/admin/onboarding/applications")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public List<ApplicationResponse> listApplications(@RequestParam(name = "type", required = false) String type) {
        return onboardingService.listApplications(type).stream().map(ApplicationResponse::from).toList();
    }

    @GetMapping("/admin/onboarding/applications/{applicationId}")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public ApplicationResponse getApplication(@PathVariable(name = "applicationId") String applicationId) {
        return ApplicationResponse.from(onboardingService.getApplication(applicationId));
    }

    @PostMapping("/admin/onboarding/applications/{applicationId}/approve")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public ApproveResponse approveApplication(
            @CurrentActor ActorContext actor,
            @PathVariable(name = "applicationId") String applicationId) {
        ApproveApplicationResult result = onboardingService.approveApplication(actor, applicationId);
        return new ApproveResponse(result.tenantId(), result.membershipId());
    }

    @PostMapping("/admin/onboarding/applications/{applicationId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public void rejectApplication(
            @CurrentActor ActorContext actor,
            @PathVariable(name = "applicationId") String applicationId,
            @Valid @RequestBody RejectApplicationRequest request) {
        onboardingService.rejectApplication(actor, applicationId, request.reason());
    }

}
