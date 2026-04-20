package io.attestry.userauth.application.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.auth.internal.UserEffectiveScopeResolver;
import io.attestry.userauth.application.policy.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.policy.command.AuthzEvaluateResult;
import io.attestry.userauth.application.policy.command.EvaluateAuthorizationService;
import io.attestry.userauth.application.policy.command.PolicyDecisionMode;
import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.application.port.membership.MembershipProjectionPort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.tenant.model.TenantType;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import io.attestry.userauth.domain.auth.model.VerificationLevel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EvaluateAuthorizationServiceTest {

    private final EvaluateAuthorizationService service = new EvaluateAuthorizationService(
        new UserEffectiveScopeResolver(
            new EmptyMembershipPort(),
            new EmptyMembershipProjectionPort()
        )
    );

    @Test
    void shouldDenyWhenRequiredScopeMissing() {
        ActorContext actor = actor("tenant-a", Set.of(PermissionCodes.OWNER_TRANSFER_CREATE));

        AuthzEvaluateResult result = service.evaluate(
            actor,
            new AuthzEvaluateCommand("tenant-a", PermissionCodes.BRAND_MINT, "resource-1", PolicyDecisionMode.TOKEN_SNAPSHOT)
        );

        assertFalse(result.allowed());
        assertEquals("FORBIDDEN_SCOPE", result.reason());
    }

    @Test
    void shouldDenyWhenTenantIsolationViolation() {
        ActorContext actor = actor("tenant-a", Set.of(PermissionCodes.BRAND_MINT));

        AuthzEvaluateResult result = service.evaluate(
            actor,
            new AuthzEvaluateCommand("tenant-b", PermissionCodes.BRAND_MINT, "resource-1", PolicyDecisionMode.TOKEN_SNAPSHOT)
        );

        assertFalse(result.allowed());
        assertEquals("TENANT_ISOLATION_VIOLATION", result.reason());
    }

    @Test
    void shouldAllowWhenScopeAndTenantBothMatch() {
        ActorContext actor = actor("tenant-a", Set.of(PermissionCodes.BRAND_MINT));

        AuthzEvaluateResult result = service.evaluate(
            actor,
            new AuthzEvaluateCommand("tenant-a", PermissionCodes.BRAND_MINT, "resource-1", PolicyDecisionMode.TOKEN_SNAPSHOT)
        );

        assertTrue(result.allowed());
        assertEquals(null, result.reason());
    }

    @Test
    void liveRecheckShouldUseTenantMembership() {
        EvaluateAuthorizationService liveService = new EvaluateAuthorizationService(
            new UserEffectiveScopeResolver(
                new SingleMembershipPort(
                    Membership.reconstitute(
                        "membership-1", "user-1", "tenant-a",
                        TenantType.BRAND, MembershipRole.ADMIN, MembershipStatus.ACTIVE,
                        TenantStatus.ACTIVE, java.util.Set.of()
                    )
                ),
                new MembershipProjectionPort() {
                    @Override
                    public Set<String> findPermissionCodesByMembershipId(String membershipId) {
                        return Set.of(PermissionCodes.TENANT_ROLE_ASSIGN);
                    }

                    @Override
                    public Set<String> findPermissionCodesByGlobalRoleCode(String roleCode) {
                        return Set.of();
                    }

                    @Override
                    public Set<String> findRoleCodesByMembershipId(String membershipId) {
                        return Set.of(RoleCodes.TENANT_OWNER);
                    }

                    @Override
                    public Set<String> findGlobalEnabledRoleCodes() {
                        return Set.of();
                    }
                }
            )
        );

        ActorContext actor = new ActorContext(
            "token-1",
            "user-1",
            "tenant-a",
            VerificationLevel.NONE,
            Set.of(),
            Instant.parse("2026-02-25T00:30:00Z")
        );

        AuthzEvaluateResult result = liveService.evaluate(
            actor,
            new AuthzEvaluateCommand("tenant-a", PermissionCodes.TENANT_ROLE_ASSIGN, "membership:target:role:TENANT_OWNER", PolicyDecisionMode.LIVE_RECHECK)
        );

        assertTrue(result.allowed());
    }

    private ActorContext actor(String tenantId, Set<String> scopes) {
        return new ActorContext(
            "token-1",
            "user-1",
            tenantId,
            VerificationLevel.NONE,
            scopes,
            Instant.parse("2026-02-25T00:30:00Z")
        );
    }

    private static class EmptyMembershipPort implements MembershipPort {
        @Override
        public Optional<Membership> findById(String membershipId) { return Optional.empty(); }

        @Override
        public Membership save(Membership membership) { return membership; }

        @Override
        public List<Membership> findByUserId(String userId) { return List.of(); }

        @Override
        public Optional<Membership> findByUserIdAndTenantId(String userId, String tenantId) { return Optional.empty(); }

        @Override
        public List<Membership> findByTenantId(String tenantId) { return List.of(); }

        @Override
        public List<Membership> findMembershipsByTenantId(String tenantId) { return List.of(); }

        @Override
        public Optional<Membership> findMembershipById(String membershipId) { return Optional.empty(); }

        @Override
        public Optional<Membership> findMembershipByMembershipIdAndUserId(String membershipId, String userId) {
            return Optional.empty();
        }

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
    }

    private static class SingleMembershipPort implements MembershipPort {

        private final Membership membership;

        private SingleMembershipPort(Membership membership) {
            this.membership = membership;
        }

        @Override
        public Optional<Membership> findById(String membershipId) {
            return membership.membershipId().equals(membershipId) ? Optional.of(membership) : Optional.empty();
        }

        @Override
        public Membership save(Membership membership) { return membership; }

        @Override
        public List<Membership> findByUserId(String userId) {
            if (membership.userId().equals(userId)) {
                return List.of(membership);
            }
            return List.of();
        }

        @Override
        public Optional<Membership> findByUserIdAndTenantId(String userId, String tenantId) {
            if (membership.userId().equals(userId)
                && membership.tenantId().equals(tenantId)) {
                return Optional.of(membership);
            }
            return Optional.empty();
        }

        @Override
        public List<Membership> findByTenantId(String tenantId) {
            return membership.tenantId().equals(tenantId) ? List.of(membership) : List.of();
        }

        @Override
        public List<Membership> findMembershipsByTenantId(String tenantId) {
            return findByTenantId(tenantId);
        }

        @Override
        public Optional<Membership> findMembershipById(String membershipId) {
            return findById(membershipId);
        }

        @Override
        public Optional<Membership> findMembershipByMembershipIdAndUserId(String membershipId, String userId) {
            if (membership.membershipId().equals(membershipId) && membership.userId().equals(userId)) {
                return Optional.of(membership);
            }
            return Optional.empty();
        }

        @Override
        public Membership updateMembership(String tenantId, String membershipId, MembershipRole role, MembershipStatus status) {
            return membership;
        }

        @Override
        public void assignRole(String membershipId, String roleCode, String assignedByUserId) {}

        @Override
        public void deletePermissionOverrides(String membershipId, Set<String> permissionCodes) {}

        @Override
        public Set<String> applyPermissionTemplateToMembership(String membershipId, String templateCode, String reason, String actorUserId, Instant now) {
            return Set.of();
        }

        @Override
        public Set<String> revokePermissionTemplateFromMembership(String membershipId, String templateCode) {
            return Set.of();
        }
    }

    private static class EmptyMembershipProjectionPort implements MembershipProjectionPort {
        @Override
        public Set<String> findPermissionCodesByMembershipId(String membershipId) {
            return Set.of();
        }

        @Override
        public Set<String> findPermissionCodesByGlobalRoleCode(String roleCode) {
            if (RoleCodes.OWNER_DEFAULT.equals(roleCode)) {
                return Set.of(
                    PermissionCodes.OWNER_TRANSFER_CREATE,
                    PermissionCodes.OWNER_TRANSFER_ACCEPT,
                    PermissionCodes.OWNER_RISK_FLAG,
                    PermissionCodes.OWNER_RISK_CLEAR,
                    PermissionCodes.OWNER_RETIRE
                );
            }
            return Set.of();
        }

        @Override
        public Set<String> findRoleCodesByMembershipId(String membershipId) {
            return Set.of();
        }

        @Override
        public Set<String> findGlobalEnabledRoleCodes() {
            return Set.of();
        }
    }
}
