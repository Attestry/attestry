package io.attestry.userauth.interfaces.onboarding;

import io.attestry.userauth.application.dto.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.CreateBrandApplicationCommand;
import io.attestry.userauth.application.dto.command.CreateRetailApplicationCommand;
import io.attestry.userauth.application.dto.command.PresignEvidenceUploadCommand;
import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.dto.result.ApproveApplicationResult;
import io.attestry.userauth.application.usecase.onboarding.OnboardingUseCase;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.interfaces.onboarding.dto.request.CompleteEvidenceUploadRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.CreateBrandApplicationRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.CreateRetailApplicationRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.PresignEvidenceUploadRequest;
import io.attestry.userauth.interfaces.onboarding.dto.request.RejectApplicationRequest;
import io.attestry.userauth.interfaces.onboarding.dto.response.ApplicationResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.ApproveResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.EvidenceBundleResponse;
import io.attestry.userauth.interfaces.onboarding.dto.response.PresignedEvidenceUploadResponse;
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
public class OnboardingHttp {

    private final OnboardingUseCase onboardingService;

    public OnboardingHttp(OnboardingUseCase onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/onboarding/evidences/presign")
    @ResponseStatus(HttpStatus.CREATED)
    public PresignedEvidenceUploadResponse presignEvidenceUpload(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody PresignEvidenceUploadRequest request
    ) {
        return PresignedEvidenceUploadResponse.from(
            onboardingService.presignEvidenceUpload(
                principal,
                new PresignEvidenceUploadCommand(request.evidenceBundleId(), request.fileName(), request.contentType())
            )
        );
    }

    @PostMapping("/onboarding/evidences/complete")
    public EvidenceBundleResponse completeEvidenceUpload(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody CompleteEvidenceUploadRequest request
    ) {
        return EvidenceBundleResponse.from(
            onboardingService.completeEvidenceUpload(
                principal,
                new CompleteEvidenceUploadCommand(request.evidenceBundleId(), request.evidenceFileId(), request.sizeBytes())
            )
        );
    }

    @PostMapping("/brand-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createBrandApplication(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody CreateBrandApplicationRequest request
    ) {
        ApplicationResult result = onboardingService.createBrandApplication(
            principal,
            new CreateBrandApplicationCommand(
                request.brandName(),
                request.country(),
                request.bizRegNo(),
                request.evidenceBundleId()
            )
        );
        return ApplicationResponse.from(result);
    }

    @GetMapping("/brand-applications/{applicationId}")
    public ApplicationResponse getBrandApplication(@PathVariable(name = "applicationId") String applicationId) {
        return ApplicationResponse.from(onboardingService.getBrandApplication(applicationId));
    }

    @GetMapping("/admin/brand-applications")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public List<ApplicationResponse> listBrandApplications() {
        return onboardingService.listBrandApplications().stream().map(ApplicationResponse::from).toList();
    }

    @PostMapping("/admin/brand-applications/{applicationId}/approve")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public ApproveResponse approveBrandApplication(@AuthenticationPrincipal AuthPrincipal principal,
                                                   @PathVariable(name = "applicationId") String applicationId) {
        ApproveApplicationResult result = onboardingService.approveBrandApplication(
                principal,
                applicationId
        );
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
        onboardingService.rejectBrandApplication(principal, applicationId, request.reason());
    }

    // -----------------------------retail----------------------------------

    @PostMapping("/tenants/{tenantId}/retail-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createRetailApplication(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestBody CreateRetailApplicationRequest request
    ) {
        ApplicationResult result = onboardingService.createRetailApplication(
            principal,
            tenantId,
            new CreateRetailApplicationCommand(
                request.retailName(),
                request.country(),
                request.bizRegNo(),
                request.evidenceBundleId()
            )
        );
        return ApplicationResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/retail-applications/{id}")
    public ApplicationResponse getRetailApplication(@PathVariable("tenantId") String tenantId, @PathVariable("id") String id) {
        return ApplicationResponse.from(onboardingService.getRetailApplication(tenantId, id));
    }

    @GetMapping("/tenants/{tenantId}/admin/retail-applications")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_MEMBERSHIP_VIEW')")
    public List<ApplicationResponse> listRetailApplications(@PathVariable("tenantId") String tenantId) {
        return onboardingService.listRetailApplications(tenantId).stream().map(ApplicationResponse::from).toList();
    }

    @PostMapping("/tenants/{tenantId}/admin/retail-applications/{id}/approve")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_GROUP_CREATE')")
    public ApproveResponse approveRetailApplication(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("tenantId") String tenantId,
            @PathVariable("id") String id
    ) {
        ApproveApplicationResult result = onboardingService.approveRetailApplication(
            principal,
            tenantId,
            id
        );
        return new ApproveResponse(result.tenantId(), result.groupId(), result.membershipId());
    }

    @PostMapping("/tenants/{tenantId}/admin/retail-applications/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_TENANT_GROUP_CREATE')")
    public void rejectRetailApplication(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable("tenantId") String tenantId,
            @PathVariable("id") String id,
            @RequestBody RejectApplicationRequest request
    ) {
        onboardingService.rejectRetailApplication(principal, tenantId, id, request.reason());
    }
}
