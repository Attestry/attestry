package io.attestry.workflow.application.support;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WorkflowAuthorizationSupport {

    private static final Set<String> PLATFORM_ADMIN_BYPASS_SCOPES = Set.of(
        "SCOPE_PLATFORM_ADMIN",
        "PLATFORM_ADMIN"
    );

    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;

    public WorkflowAuthorizationSupport(EvaluateAuthorizationUseCase evaluateAuthorizationUseCase) {
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
    }

    public void assertTenantContext(AuthPrincipal principal, String tenantId) {
        if (principal.scopes() != null && principal.scopes().stream().anyMatch(PLATFORM_ADMIN_BYPASS_SCOPES::contains)) {
            return;
        }
        if (principal.tenantId() == null || !principal.tenantId().equals(tenantId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant access denied");
        }
    }

    public void assertPermissionOnly(AuthPrincipal principal, String action, String resourceRef) {
        assertLivePermission(principal, principal.tenantId(), action, resourceRef);
    }

    public void assertLivePermission(AuthPrincipal principal, String tenantId, String action, String resourceRef) {
        if (principal.scopes() != null && principal.scopes().stream().anyMatch(PLATFORM_ADMIN_BYPASS_SCOPES::contains)) {
            return;
        }
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            ActorContext.from(principal),
            new AuthzEvaluateCommand(tenantId, action, resourceRef, PolicyDecisionMode.LIVE_RECHECK)
        );
        if (!decision.allowed()) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Action denied by live policy check");
        }
    }
}
