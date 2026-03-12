package io.attestry.workflow.application.servicerequest.policy;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestAccessPolicy {

    private final WorkflowAuthorizationSupport authorizationSupport;

    public ServiceRequestAccessPolicy(WorkflowAuthorizationSupport authorizationSupport) {
        this.authorizationSupport = authorizationSupport;
    }

    public void assertOwnerCreatePermission(AuthPrincipal principal, String resourceRef) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_SERVICE_CREATE, resourceRef);
    }

    public void assertProviderCompletePermission(AuthPrincipal principal, String tenantId, String resourceRef) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.SERVICE_COMPLETE, resourceRef);
    }

    public void assertProviderRequestAccess(String tenantId, ServiceRequest request, String action) {
        if (!tenantId.equals(request.providerTenantId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant service request access denied");
        }
    }

    public void assertOwnerRequestAccess(AuthPrincipal principal, ServiceRequest request, String action) {
        if (!principal.userId().equals(request.ownerUserId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only the owner can " + action + " a service request");
        }
    }

    public void assertOwnerConsentAccess(AuthPrincipal principal, String currentOwnerId, String action) {
        if (!principal.userId().equals(currentOwnerId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only the passport owner can " + action);
        }
    }
}
