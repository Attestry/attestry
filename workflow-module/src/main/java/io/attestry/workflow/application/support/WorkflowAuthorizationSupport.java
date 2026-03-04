package io.attestry.workflow.application.support;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import org.springframework.stereotype.Component;

@Component
public class WorkflowAuthorizationSupport {

    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;

    public WorkflowAuthorizationSupport(EvaluateAuthorizationUseCase evaluateAuthorizationUseCase) {
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
    }

    public void assertTenantContext(AuthPrincipal principal, String tenantId) {
        if (principal.tenantId() == null || !principal.tenantId().equals(tenantId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant access denied");
        }
    }

    public void assertTenantAndGroupContext(AuthPrincipal principal, String tenantId, String groupId) {
        if (principal.tenantId() == null || !principal.tenantId().equals(tenantId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant access denied");
        }
        if (principal.groupId() == null || !principal.groupId().equals(groupId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-group access denied");
        }
    }

    public void assertPermissionOnly(AuthPrincipal principal, String action, String resourceRef) {
        assertLivePermission(principal, principal.tenantId(), action, resourceRef);
    }

    public void assertLivePermission(AuthPrincipal principal, String tenantId, String action, String resourceRef) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            ActorContext.from(principal),
            new AuthzEvaluateCommand(tenantId, action, resourceRef, PolicyDecisionMode.LIVE_RECHECK)
        );
        if (!decision.allowed()) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Action denied by live policy check");
        }
    }
}
