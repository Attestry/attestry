package io.attestry.userauth.application.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.port.MembershipPermissionQueryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.auth.model.PermissionCodes;
import io.attestry.userauth.domain.auth.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.user.enums.VerificationLevel;
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
        AuthPrincipal principal = principal("tenant-a", Set.of(PermissionCodes.OWNER_TRANSFER_CREATE));

        AuthzEvaluateResult result = service.evaluate(
            principal,
            new AuthzEvaluateCommand("tenant-a", PermissionCodes.BRAND_MINT, "resource-1", PolicyDecisionMode.TOKEN_SNAPSHOT)
        );

        assertFalse(result.allowed());
        assertEquals("FORBIDDEN_SCOPE", result.reason());
    }

    @Test
    void shouldDenyWhenTenantIsolationViolation() {
        AuthPrincipal principal = principal("tenant-a", Set.of(PermissionCodes.BRAND_MINT));

        AuthzEvaluateResult result = service.evaluate(
            principal,
            new AuthzEvaluateCommand("tenant-b", PermissionCodes.BRAND_MINT, "resource-1", PolicyDecisionMode.TOKEN_SNAPSHOT)
        );

        assertFalse(result.allowed());
        assertEquals("TENANT_ISOLATION_VIOLATION", result.reason());
    }

    @Test
    void shouldAllowWhenScopeAndTenantBothMatch() {
        AuthPrincipal principal = principal("tenant-a", Set.of(PermissionCodes.BRAND_MINT));

        AuthzEvaluateResult result = service.evaluate(
            principal,
            new AuthzEvaluateCommand("tenant-a", PermissionCodes.BRAND_MINT, "resource-1", PolicyDecisionMode.TOKEN_SNAPSHOT)
        );

        assertTrue(result.allowed());
        assertEquals(null, result.reason());
    }

    private AuthPrincipal principal(String tenantId, Set<String> scopes) {
        return new AuthPrincipal(
            "token-1",
            "user-1",
            tenantId,
            "group-1",
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
        public Optional<Membership> findByUserIdAndContext(String userId, String tenantId, String groupId) {
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
