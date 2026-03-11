package io.attestry.userauth.application.onboarding;

import io.attestry.userauth.application.port.MembershipPort;
import io.attestry.userauth.application.port.TenantRoleTemplateBindingPort;
import io.attestry.userauth.application.port.TenantRepositoryPort;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.policy.DefaultMembershipRolePolicy;
import io.attestry.userauth.domain.onboarding.policy.OnboardingTemplateBindingPolicy;
import io.attestry.userauth.domain.tenant.model.Tenant;
import io.attestry.userauth.domain.tenant.model.TenantType;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OnboardingProvisioningService {

    private final TenantRepositoryPort tenantRepository;
    private final MembershipPort membershipPort;
    private final TenantRoleTemplateBindingPort tenantRoleTemplateBindingPort;
    private final OnboardingTemplateBindingPolicy templateBindingPolicy = new OnboardingTemplateBindingPolicy();
    private final Clock clock;
    private final EntityManager entityManager;


    public ProvisioningResult provision(
        TenantType type,
        String applicantUserId,
        String orgName,
        String country,
        String address,
        String actorUserId
    ) {
        Tenant tenant = tenantRepository.save(Tenant.create(orgName, country, address, type));

        Membership membership = Membership.create(
            applicantUserId, tenant.tenantId(),
            type, MembershipRole.ADMIN,
            TenantStatus.ACTIVE
        );
        membershipPort.save(membership);

        String roleCode = DefaultMembershipRolePolicy.resolveGlobalRoleCode(membership.role(), membership.groupType());
        membershipPort.assignRole(membership.membershipId(), roleCode, actorUserId);
        entityManager.flush();

        applyTemplateBindings(tenant.tenantId(), type, actorUserId, Instant.now(clock));

        return new ProvisioningResult(tenant.tenantId(), membership.membershipId());
    }

    private void applyTemplateBindings(String tenantId, TenantType type, String actorUserId, Instant now) {
        List<OnboardingTemplateBindingPolicy.TemplateBindingRule> rules = templateBindingPolicy.resolveDefaultBindings(type);
        for (OnboardingTemplateBindingPolicy.TemplateBindingRule rule : rules) {
            tenantRoleTemplateBindingPort.bindTemplateToTenantRole(
                tenantId,
                rule.roleCode(),
                rule.templateCode(),
                actorUserId,
                now
            );
        }
    }

    public record ProvisioningResult(String tenantId, String membershipId) {}
}
