package io.attestry.userauth.application.onboarding;

import io.attestry.userauth.application.port.MembershipProvisioningRepositoryPort;
import io.attestry.userauth.application.port.TemplateAdminRepositoryPort;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.authorization.policy.SystemPermissionTemplateCatalog;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.organization.model.Group;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.organization.model.Tenant;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import io.attestry.userauth.domain.organization.repository.TenantRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class OnboardingProvisioningService {

    private final TenantRepository tenantRepository;
    private final MembershipProvisioningRepositoryPort membershipProvisioningRepository;
    private final TemplateAdminRepositoryPort templateAdminRepository;
    private final Clock clock;

    public OnboardingProvisioningService(
        TenantRepository tenantRepository,
        MembershipProvisioningRepositoryPort membershipProvisioningRepository,
        TemplateAdminRepositoryPort templateAdminRepository,
        Clock clock
    ) {
        this.tenantRepository = tenantRepository;
        this.membershipProvisioningRepository = membershipProvisioningRepository;
        this.templateAdminRepository = templateAdminRepository;
        this.clock = clock;
    }

    public ProvisioningResult provision(
        GroupType type,
        String applicantUserId,
        String orgName,
        String country,
        String actorUserId
    ) {
        Tenant tenant = Tenant.create(orgName, country);
        Group group = tenant.addGroup(type);
        tenantRepository.save(tenant);

        Membership membership = Membership.create(
            applicantUserId, group.groupId(), tenant.tenantId(),
            type, MembershipRole.ADMIN,
            GroupStatus.ACTIVE, TenantStatus.ACTIVE
        );
        membershipProvisioningRepository.save(membership);
        membershipProvisioningRepository.assignRole(membership.membershipId(), RoleCodes.TENANT_OWNER, actorUserId);

        templateAdminRepository.bindTemplateToTenantRole(
            tenant.tenantId(),
            RoleCodes.TENANT_OWNER,
            SystemPermissionTemplateCatalog.TEMPLATE_TENANT_OWNER_CORE,
            actorUserId,
            Instant.now(clock)
        );
        templateAdminRepository.bindTemplateToTenantRole(
            tenant.tenantId(),
            RoleCodes.TENANT_OWNER,
            SystemPermissionTemplateCatalog.TEMPLATE_TENANT_READ_ONLY,
            actorUserId,
            Instant.now(clock)
        );
        templateAdminRepository.bindTemplateToTenantRole(
            tenant.tenantId(),
            RoleCodes.TENANT_OPERATOR,
            SystemPermissionTemplateCatalog.TEMPLATE_TENANT_READ_ONLY,
            actorUserId,
            Instant.now(clock)
        );
        templateAdminRepository.bindTemplateToTenantRole(
            tenant.tenantId(),
            RoleCodes.TENANT_STAFF,
            SystemPermissionTemplateCatalog.TEMPLATE_TENANT_READ_ONLY,
            actorUserId,
            Instant.now(clock)
        );

        return new ProvisioningResult(tenant.tenantId(), group.groupId(), membership.membershipId());
    }

    public record ProvisioningResult(String tenantId, String groupId, String membershipId) {}
}
