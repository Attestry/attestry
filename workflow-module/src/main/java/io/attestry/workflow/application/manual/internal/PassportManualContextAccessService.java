package io.attestry.workflow.application.manual.internal;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.port.manual.PassportManualReadPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PassportManualContextAccessService {

    private final PassportManualReadPort passportManualReadPort;
    private final WorkflowAuthorizationSupport authorizationSupport;

    public PassportManualReadPort.PassportManualContext loadAuthorizedContext(
        WorkflowActorContext principal,
        String tenantId,
        String passportId,
        String permissionActionPrefix
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(
            principal,
            tenantId,
            PermissionCodes.BRAND_RELEASE,
            permissionActionPrefix + passportId
        );

        PassportManualReadPort.PassportManualContext context = passportManualReadPort.findContext(passportId)
            .orElseThrow(() -> new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "Product information not found."
            ));
        if (!tenantId.equals(context.tenantId())) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.TENANT_ISOLATION_VIOLATION,
                "Cross-tenant access denied"
            );
        }
        return context;
    }
}
