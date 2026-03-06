package io.attestry.userauth.application.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.port.MembershipPermissionQueryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.organization.model.TenantType;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import io.attestry.userauth.domain.identity.model.VerificationLevel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EvaluateAuthorizationServiceTest {

    private final EvaluateAuthorizationService service = new EvaluateAuthorizationService(
        new EmptyMembershipRepository(),
        new EmptyMembershipPermissionQueryRepository()
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
            new SingleMembershipRepository(
                Membership.reconstitute(
                    "membership-1", "user-1", "tenant-a",
                    TenantType.BRAND, MembershipRole.ADMIN, MembershipStatus.ACTIVE,
                    TenantStatus.ACTIVE, java.util.Set.of()
                )
            ),
            new MembershipPermissionQueryPort() {
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
            }
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

    private static class EmptyMembershipRepository implements MembershipRepositoryPort {
        @Override
        public List<Membership> findByUserId(String userId) {
            return List.of();
        }

        @Override
        public Optional<Membership> findByUserIdAndTenantId(String userId, String tenantId) {
            return Optional.empty();
        }
    }

    private static class SingleMembershipRepository implements MembershipRepositoryPort {

        private final Membership membership;

        private SingleMembershipRepository(Membership membership) {
            this.membership = membership;
        }

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
    }

    private static class EmptyMembershipPermissionQueryRepository implements MembershipPermissionQueryPort {
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
                    PermissionCodes.OWNER_RISK_CLEAR
                );
            }
            return Set.of();
        }

        @Override
        public Set<String> findRoleCodesByMembershipId(String membershipId) {
            return Set.of();
        }
    }
}
