package io.attestry.userauth.application.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.dto.view.MembershipView;
import io.attestry.userauth.application.membership.assembler.MembershipQueryViewAssembler;
import io.attestry.userauth.application.membership.query.MembershipQueryService;
import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.application.port.membership.MembershipProjectionPort;
import io.attestry.userauth.application.port.tenant.TenantRepositoryPort;
import io.attestry.userauth.application.port.template.TenantRoleTemplateBindingPort;
import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.membership.policy.RoleAssignmentDecisionPolicy;
import io.attestry.userauth.domain.membership.policy.RoleAssignmentPolicy;
import io.attestry.userauth.domain.membership.service.RoleAssignmentDomainService;
import io.attestry.userauth.domain.tenant.model.TenantType;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class MembershipQueryServiceTest {

    @Test
    void shouldMapMembershipsToViewWithEffectiveScopes() {
        MembershipPort membershipPort = new MembershipPort() {
            @Override
            public Optional<Membership> findById(String membershipId) { return Optional.empty(); }

            @Override
            public Membership save(Membership membership) { return membership; }

            @Override
            public List<Membership> findByUserId(String userId) {
                return List.of(Membership.reconstitute(
                    "m1", userId, "t1",
                    TenantType.RETAIL, MembershipRole.OPERATOR, MembershipStatus.ACTIVE,
                    TenantStatus.ACTIVE, Set.of()
                ));
            }

            @Override
            public Optional<Membership> findByUserIdAndTenantId(String userId, String tenantId) {
                return Optional.empty();
            }

            @Override
            public List<Membership> findByTenantId(String tenantId) { return List.of(); }

            @Override
            public List<Membership> findMembershipsByTenantId(String tenantId) { return List.of(); }

            @Override
            public Optional<Membership> findMembershipById(String membershipId) { return Optional.empty(); }

            @Override
            public Membership updateMembership(String tenantId, String membershipId, MembershipRole role, MembershipStatus status) { return null; }

            @Override
            public void assignRole(String membershipId, String roleCode, String assignedByUserId) {}

            @Override
            public void deletePermissionOverrides(String membershipId, Set<String> permissionCodes) {}

            @Override
            public Set<String> applyPermissionTemplateToMembership(String membershipId, String templateCode, String reason, String actorUserId, Instant now) { return Set.of(); }

            @Override
            public Set<String> revokePermissionTemplateFromMembership(String membershipId, String templateCode) { return Set.of(); }
        };

        MembershipProjectionPort membershipProjectionPort = new MembershipProjectionPort() {
            @Override
            public Set<String> findPermissionCodesByMembershipId(String membershipId) {
                return Set.of(PermissionCodes.RETAIL_TRANSFER_CREATE);
            }

            @Override
            public Set<String> findPermissionCodesByGlobalRoleCode(String roleCode) { return Set.of(); }

            @Override
            public Set<String> findRoleCodesByMembershipId(String membershipId) { return Set.of("TENANT_OPERATOR"); }

            @Override
            public Set<String> findGlobalEnabledRoleCodes() { return Set.of(); }
        };

        TenantRepositoryPort tenantRepository = new TenantRepositoryPort() {
            @Override
            public io.attestry.userauth.domain.tenant.model.Tenant save(io.attestry.userauth.domain.tenant.model.Tenant tenant) {
                return tenant;
            }

            @Override
            public Optional<io.attestry.userauth.domain.tenant.model.Tenant> findById(String tenantId) {
                return Optional.empty();
            }

            @Override
            public Page<io.attestry.userauth.domain.tenant.model.Tenant> findPage(
                    io.attestry.userauth.domain.tenant.model.TenantType type,
                    io.attestry.userauth.domain.tenant.model.TenantStatus status,
                    String name,
                    Pageable pageable) {
                return new PageImpl<>(List.of());
            }
        };

        UserAccountRepositoryPort userAccountRepository = new UserAccountRepositoryPort() {
            @Override
            public Optional<io.attestry.userauth.domain.identity.model.UserAccount> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public Optional<io.attestry.userauth.domain.identity.model.UserAccount> findById(String userId) {
                return Optional.empty();
            }

            @Override
            public io.attestry.userauth.domain.identity.model.UserAccount save(
                    io.attestry.userauth.domain.identity.model.UserAccount userAccount) {
                return userAccount;
            }
        };

        TenantRoleTemplateBindingPort tenantRoleTemplateBindingPort = new TenantRoleTemplateBindingPort() {
            @Override
            public TenantRoleTemplateBindingView bindTemplateToTenantRole(
                    String tenantId,
                    String roleCode,
                    String templateCode,
                    String actorUserId,
                    Instant now) { return null; }

            @Override
            public List<TenantRoleTemplateBindingView> findTenantRoleTemplateBindings(String tenantId) { return List.of(); }

            @Override
            public void disableTenantRoleTemplateBinding(String tenantId, String roleCode, String templateCode, Instant now) {}
        };

        RoleAssignmentDomainService roleAssignmentDomainService =
                new RoleAssignmentDomainService(new RoleAssignmentDecisionPolicy(new RoleAssignmentPolicy() {
                    @Override
                    public boolean canAssign(Set<String> actorRoleCodes, String targetRoleCode) {
                        return true;
                    }

                    @Override
                    public boolean isSensitiveRole(String roleCode) {
                        return false;
                    }

                    @Override
                    public boolean requiresLiveRecheck(String roleCode) {
                        return false;
                    }

                    @Override
                    public boolean isSelfEscalationDenied(
                            String actorMembershipId,
                            String targetMembershipId,
                            String roleCode) {
                        return false;
                    }
                }));

        MembershipQueryViewAssembler viewAssembler = new MembershipQueryViewAssembler(
                membershipProjectionPort,
                tenantRepository,
                userAccountRepository);

        MembershipQueryService service = new MembershipQueryService(
                membershipPort,
                membershipProjectionPort,
                userAccountRepository,
                tenantRoleTemplateBindingPort,
                null,
                null,
                viewAssembler,
                roleAssignmentDomainService);
        List<MembershipView> views = service.getMemberships("u1");

        assertEquals(1, views.size());
        MembershipView view = views.getFirst();
        assertEquals("m1", view.membershipId());
        assertEquals(List.of("TENANT_OPERATOR"), view.roleCodes());
        assertTrue(view.effectiveScopes().contains(PermissionCodes.RETAIL_TRANSFER_CREATE));
    }
}
