package io.attestry.workflow.domain.delegation.policy;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class DelegationGrantPolicy {

    private static final String RESOURCE_PASSPORT = "PASSPORT";
    private static final String PERMISSION_RETAIL_TRANSFER_CREATE = "RETAIL_TRANSFER_CREATE";
    private static final String ASSET_STATE_VOIDED = "VOIDED";
    private static final Duration MAX_EXPIRY_WINDOW = Duration.ofDays(90);

    public void assertGrantable(DelegationGrantContext context) {
        assertSupportedResourceAndPermission(context.resourceType(), context.permissionCode());
        assertPassportDelegatable(context);
        assertExpiryValid(context.expiresAt(), context.now());
        assertNoDuplicate(context.activeDelegationExists());
    }

    private void assertSupportedResourceAndPermission(String resourceType, String permissionCode) {
        if (!RESOURCE_PASSPORT.equals(resourceType)) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST,
                "Only PASSPORT delegation is supported in v1");
        }
        if (!PERMISSION_RETAIL_TRANSFER_CREATE.equals(permissionCode)) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST,
                "permissionCode must be RETAIL_TRANSFER_CREATE for PASSPORT delegation");
        }
    }

    private void assertPassportDelegatable(DelegationGrantContext context) {
        if (context.passportTenantId() == null) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found");
        }
        if (!context.sourceTenantId().equals(context.passportTenantId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE,
                "Source tenant does not own the passport");
        }
        if (ASSET_STATE_VOIDED.equals(context.passportAssetState())) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE,
                "VOIDED passport cannot be delegated");
        }
    }

    private void assertExpiryValid(Instant expiresAt, Instant now) {
        if (expiresAt == null) {
            return;
        }
        if (!expiresAt.isAfter(now)) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST,
                "expiresAt must be in the future");
        }
        if (expiresAt.isAfter(now.plus(MAX_EXPIRY_WINDOW))) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST,
                "expiresAt exceeds max 90 days");
        }
    }

    private void assertNoDuplicate(boolean activeDelegationExists) {
        if (activeDelegationExists) {
            throw new WorkflowDomainException(WorkflowErrorCode.DELEGATION_ALREADY_ACTIVE,
                "Active delegation already exists");
        }
    }

    public record DelegationGrantContext(
        String sourceTenantId,
        String resourceType,
        String permissionCode,
        Instant expiresAt,
        String passportTenantId,
        String passportAssetState,
        boolean activeDelegationExists,
        Instant now
    ) {
    }
}
