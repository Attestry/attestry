package io.attestry.userauth.application.onboarding;

import io.attestry.userauth.application.port.OnboardingRepositoryPort;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.policy.TenantIsolationPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApplicationService {

    private final OnboardingRepositoryPort onboardingRepository;
    private final Clock clock;

    public OnboardingApplicationService(
        OnboardingRepositoryPort onboardingRepository,
        Clock clock
    ) {
        this.onboardingRepository = onboardingRepository;
        this.clock = clock;
    }

    @Transactional
    public OrganizationApplication createBrandApplication(AuthPrincipal principal, CreateBrandApplicationCommand command) {
        OrganizationApplication application = OrganizationApplication.createBrand(
            principal.userId(),
            command.brandName(),
            command.country(),
            command.bizRegNo(),
            command.evidenceGroupId()
        );
        return onboardingRepository.saveApplication(application);
    }

    @Transactional(readOnly = true)
    public List<OrganizationApplication> listBrandApplications() {
        return onboardingRepository.findApplicationsByType(GroupType.BRAND);
    }

    @Transactional(readOnly = true)
    public OrganizationApplication getBrandApplication(String applicationId) {
        OrganizationApplication app = onboardingRepository.findApplicationById(applicationId)
            .orElseThrow(() -> new DomainException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
        if (app.type() != GroupType.BRAND) {
            throw new DomainException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found");
        }
        return app;
    }

    @Transactional
    public ApproveApplicationResult approveBrandApplication(AuthPrincipal principal, String applicationId) {
        OrganizationApplication app = getBrandApplication(applicationId);
        app.assertPending();

        String tenantId = UUID.randomUUID().toString();
        String groupId = UUID.randomUUID().toString();
        String membershipId = UUID.randomUUID().toString();

        onboardingRepository.createTenant(tenantId, app.orgName(), app.country());
        onboardingRepository.createGroup(groupId, tenantId, GroupType.BRAND);
        onboardingRepository.createMembershipAsAdmin(membershipId, app.applicantUserId(), groupId, tenantId, GroupType.BRAND);

        onboardingRepository.saveApplication(app.approve(principal.userId(), tenantId, Instant.now(clock)));

        return new ApproveApplicationResult(tenantId, groupId, membershipId);
    }

    @Transactional
    public void rejectBrandApplication(AuthPrincipal principal, String applicationId, String rejectReason) {
        OrganizationApplication app = getBrandApplication(applicationId);
        onboardingRepository.saveApplication(app.reject(principal.userId(), rejectReason, Instant.now(clock)));
    }

    @Transactional
    public OrganizationApplication createRetailApplication(
        AuthPrincipal principal,
        String tenantId,
        CreateRetailApplicationCommand command
    ) {
        OrganizationApplication application = OrganizationApplication.createRetail(
            principal.userId(),
            tenantId,
            command.retailName(),
            command.country(),
            command.bizRegNo(),
            command.evidenceGroupId()
        );
        return onboardingRepository.saveApplication(application);
    }

    @Transactional(readOnly = true)
    public OrganizationApplication getRetailApplication(String tenantId, String applicationId) {
        OrganizationApplication app = onboardingRepository.findApplicationById(applicationId)
            .orElseThrow(() -> new DomainException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));

        if (app.type() != GroupType.RETAIL || !tenantId.equals(app.tenantId())) {
            throw new DomainException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found");
        }
        return app;
    }

    @Transactional(readOnly = true)
    public List<OrganizationApplication> listRetailApplications(String tenantId) {
        return onboardingRepository.findApplicationsByTenantAndType(tenantId, GroupType.RETAIL);
    }

    @Transactional
    public ApproveApplicationResult approveRetailApplication(AuthPrincipal principal, String tenantId, String applicationId) {
        assertTenantIsolation(principal, tenantId);
        OrganizationApplication app = getRetailApplication(tenantId, applicationId);
        app.assertPending();

        String groupId = UUID.randomUUID().toString();
        String membershipId = UUID.randomUUID().toString();

        onboardingRepository.createGroup(groupId, tenantId, GroupType.RETAIL);
        onboardingRepository.createMembershipAsAdmin(membershipId, app.applicantUserId(), groupId, tenantId, GroupType.RETAIL);

        onboardingRepository.saveApplication(app.approve(principal.userId(), tenantId, Instant.now(clock)));

        return new ApproveApplicationResult(tenantId, groupId, membershipId);
    }

    @Transactional
    public void rejectRetailApplication(AuthPrincipal principal, String tenantId, String applicationId, String rejectReason) {
        assertTenantIsolation(principal, tenantId);
        OrganizationApplication app = getRetailApplication(tenantId, applicationId);
        onboardingRepository.saveApplication(app.reject(principal.userId(), rejectReason, Instant.now(clock)));
    }

    private void assertTenantIsolation(AuthPrincipal principal, String tenantId) {
        if (!TenantIsolationPolicy.isIsolated(principal.tenantId(), tenantId)) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant access denied");
        }
    }

    public record CreateBrandApplicationCommand(
        String brandName,
        String country,
        String bizRegNo,
        String evidenceGroupId
    ) {
    }

    public record CreateRetailApplicationCommand(
        String retailName,
        String country,
        String bizRegNo,
        String evidenceGroupId
    ) {
    }

    public record ApproveApplicationResult(String tenantId, String groupId, String membershipId) {
    }
}
