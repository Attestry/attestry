package io.attestry.userauth.application.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.dto.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.AuthzEvaluateResult;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.auth.model.Scope;
import io.attestry.userauth.domain.user.enums.VerificationLevel;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EvaluateAuthorizationServiceTest {

    private final EvaluateAuthorizationService service = new EvaluateAuthorizationService();

    @Test
    void shouldDenyWhenRequiredScopeMissing() {
        AuthPrincipal principal = principal("tenant-a", Set.of(Scope.OWNER_TRANSFER_CREATE));

        AuthzEvaluateResult result = service.evaluate(
            principal,
            new AuthzEvaluateCommand("tenant-a", Scope.BRAND_MINT, "resource-1")
        );

        assertFalse(result.allowed());
        assertEquals("FORBIDDEN_SCOPE", result.reason());
    }

    @Test
    void shouldDenyWhenTenantIsolationViolation() {
        AuthPrincipal principal = principal("tenant-a", Set.of(Scope.BRAND_MINT));

        AuthzEvaluateResult result = service.evaluate(
            principal,
            new AuthzEvaluateCommand("tenant-b", Scope.BRAND_MINT, "resource-1")
        );

        assertFalse(result.allowed());
        assertEquals("TENANT_ISOLATION_VIOLATION", result.reason());
    }

    @Test
    void shouldAllowWhenScopeAndTenantBothMatch() {
        AuthPrincipal principal = principal("tenant-a", Set.of(Scope.BRAND_MINT));

        AuthzEvaluateResult result = service.evaluate(
            principal,
            new AuthzEvaluateCommand("tenant-a", Scope.BRAND_MINT, "resource-1")
        );

        assertTrue(result.allowed());
        assertEquals(null, result.reason());
    }

    private AuthPrincipal principal(String tenantId, Set<Scope> scopes) {
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
}
