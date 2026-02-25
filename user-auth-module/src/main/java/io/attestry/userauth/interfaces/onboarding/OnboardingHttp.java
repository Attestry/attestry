package io.attestry.userauth.interfaces.onboarding;

import io.attestry.userauth.application.onboarding.OnboardingApplicationService;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.security.AuthPrincipalResolver;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    private final OnboardingApplicationService onboardingService;

    public OnboardingHttp(OnboardingApplicationService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/brand-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createBrandApplication(Authentication authentication, @RequestBody CreateBrandApplicationRequest request) {
        OrganizationApplication app = onboardingService.createBrandApplication(
            AuthPrincipalResolver.resolve(authentication),
            new OnboardingApplicationService.CreateBrandApplicationCommand(
                request.brandName(),
                request.country(),
                request.bizRegNo(),
                request.evidenceGroupId()
            )
        );
        return ApplicationResponse.from(app);
    }

    @GetMapping("/brand-applications/{id}")
    public ApplicationResponse getBrandApplication(@PathVariable("id") String id) {
        return ApplicationResponse.from(onboardingService.getBrandApplication(id));
    }

    @GetMapping("/admin/brand-applications")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public List<ApplicationResponse> listBrandApplications() {
        return onboardingService.listBrandApplications().stream().map(ApplicationResponse::from).toList();
    }

    @PostMapping("/admin/brand-applications/{id}/approve")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public ApproveResponse approveBrandApplication(Authentication authentication, @PathVariable("id") String id) {
        OnboardingApplicationService.ApproveApplicationResult result = onboardingService.approveBrandApplication(
            AuthPrincipalResolver.resolve(authentication),
            id
        );
        return new ApproveResponse(result.tenantId(), result.groupId(), result.membershipId());
    }

    @PostMapping("/admin/brand-applications/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_TENANT_CREATE_APPROVE')")
    public void rejectBrandApplication(
        Authentication authentication,
        @PathVariable("id") String id,
        @RequestBody RejectApplicationRequest request
    ) {
        onboardingService.rejectBrandApplication(AuthPrincipalResolver.resolve(authentication), id, request.reason());
    }

    @PostMapping("/tenants/{tenantId}/retail-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createRetailApplication(
        Authentication authentication,
        @PathVariable("tenantId") String tenantId,
        @RequestBody CreateRetailApplicationRequest request
    ) {
        OrganizationApplication app = onboardingService.createRetailApplication(
            AuthPrincipalResolver.resolve(authentication),
            tenantId,
            new OnboardingApplicationService.CreateRetailApplicationCommand(
                request.retailName(),
                request.country(),
                request.bizRegNo(),
                request.evidenceGroupId()
            )
        );
        return ApplicationResponse.from(app);
    }

    @GetMapping("/tenants/{tenantId}/retail-applications/{id}")
    public ApplicationResponse getRetailApplication(@PathVariable("tenantId") String tenantId, @PathVariable("id") String id) {
        return ApplicationResponse.from(onboardingService.getRetailApplication(tenantId, id));
    }

    @GetMapping("/tenants/{tenantId}/admin/retail-applications")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ADMIN')")
    public List<ApplicationResponse> listRetailApplications(@PathVariable("tenantId") String tenantId) {
        return onboardingService.listRetailApplications(tenantId).stream().map(ApplicationResponse::from).toList();
    }

    @PostMapping("/tenants/{tenantId}/admin/retail-applications/{id}/approve")
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ADMIN')")
    public ApproveResponse approveRetailApplication(
        Authentication authentication,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String id
    ) {
        OnboardingApplicationService.ApproveApplicationResult result = onboardingService.approveRetailApplication(
            AuthPrincipalResolver.resolve(authentication),
            tenantId,
            id
        );
        return new ApproveResponse(result.tenantId(), result.groupId(), result.membershipId());
    }

    @PostMapping("/tenants/{tenantId}/admin/retail-applications/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_TENANT_ADMIN')")
    public void rejectRetailApplication(
        Authentication authentication,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("id") String id,
        @RequestBody RejectApplicationRequest request
    ) {
        onboardingService.rejectRetailApplication(AuthPrincipalResolver.resolve(authentication), tenantId, id, request.reason());
    }

    public record CreateBrandApplicationRequest(
        String brandName,
        String country,
        String bizRegNo,
        String evidenceGroupId
    ) {
    }

    public record CreateRetailApplicationRequest(
        String retailName,
        String country,
        String bizRegNo,
        String evidenceGroupId
    ) {
    }

    public record RejectApplicationRequest(String reason) {
    }

    public record ApproveResponse(String tenantId, String groupId, String membershipId) {
    }

    public record ApplicationResponse(
        String applicationId,
        String type,
        String applicantUserId,
        String tenantId,
        String orgName,
        String country,
        String status,
        String rejectReason
    ) {
        static ApplicationResponse from(OrganizationApplication app) {
            return new ApplicationResponse(
                app.applicationId(),
                app.type().name(),
                app.applicantUserId(),
                app.tenantId(),
                app.orgName(),
                app.country(),
                app.status().name(),
                app.rejectReason()
            );
        }
    }
}
