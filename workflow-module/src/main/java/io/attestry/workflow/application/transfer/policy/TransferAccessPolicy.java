package io.attestry.workflow.application.transfer.policy;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.transfer.TransferProductReadPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import org.springframework.stereotype.Component;

@Component
public class TransferAccessPolicy {

    private final WorkflowAuthorizationSupport authorizationSupport;
    private final TransferProductReadPort productReadPort;

    public TransferAccessPolicy(
        WorkflowAuthorizationSupport authorizationSupport,
        TransferProductReadPort productReadPort
    ) {
        this.authorizationSupport = authorizationSupport;
        this.productReadPort = productReadPort;
    }

    public void assertCreateC2CAccess(AuthPrincipal principal, String passportId) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_TRANSFER_CREATE, "transfer:create:" + passportId);
    }

    public void assertCreateB2CAccess(AuthPrincipal principal, String tenantId, String passportId) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.RETAIL_TRANSFER_CREATE, "transfer:create:" + passportId);
    }

    public void assertFindPendingC2CAccess(AuthPrincipal principal, String passportId) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_TRANSFER_CREATE, "transfer:pending:" + passportId);
        String currentOwnerId = productReadPort.findCurrentOwnerId(passportId).orElse(null);
        if (!principal.userId().equals(currentOwnerId)) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.FORBIDDEN_SCOPE,
                "Only current owner can view pending C2C transfer"
            );
        }
    }

    public void assertFindPendingB2CAccess(AuthPrincipal principal, String tenantId, String passportId) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.RETAIL_TRANSFER_CREATE, "transfer:pending:" + passportId);
        if (!productReadPort.hasRetailPermission(passportId, tenantId)) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.FORBIDDEN_SCOPE,
                "Retail tenant does not have permission for this passport"
            );
        }
    }

    public void assertAcceptAccess(AuthPrincipal principal, String transferId) {
        authorizationSupport.assertPermissionOnly(principal, PermissionCodes.OWNER_TRANSFER_ACCEPT, "transfer:accept:" + transferId);
    }
}
