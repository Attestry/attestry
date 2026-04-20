package io.attestry.workflow.domain.delegation.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.delegation.policy.DelegationGrantPolicy.DelegationGrantContext;
import io.attestry.workflow.domain.passport.model.WorkflowAssetState;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DelegationGrantPolicyTest {

    private final DelegationGrantPolicy policy = new DelegationGrantPolicy();

    private static final Instant NOW = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant VALID_EXPIRY = Instant.parse("2026-05-01T00:00:00Z");

    private DelegationGrantContext validContext() {
        return new DelegationGrantContext(
            "tenant-1", "PASSPORT", "RETAIL_TRANSFER_CREATE",
            VALID_EXPIRY, "tenant-1", WorkflowAssetState.ACTIVE, false, NOW
        );
    }

    // ── success ──

    @Test
    void grantable_success() {
        assertDoesNotThrow(() -> policy.assertGrantable(validContext()));
    }

    @Test
    void grantable_nullExpiry_success() {
        DelegationGrantContext context = new DelegationGrantContext(
            "tenant-1", "PASSPORT", "RETAIL_TRANSFER_CREATE",
            null, "tenant-1", WorkflowAssetState.ACTIVE, false, NOW
        );
        assertDoesNotThrow(() -> policy.assertGrantable(context));
    }

    // ── resource & permission ──

    @Test
    void failsWhenResourceTypeNotPassport() {
        DelegationGrantContext context = new DelegationGrantContext(
            "tenant-1", "OTHER", "RETAIL_TRANSFER_CREATE",
            VALID_EXPIRY, "tenant-1", WorkflowAssetState.ACTIVE, false, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertGrantable(context));
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void failsWhenPermissionCodeInvalid() {
        DelegationGrantContext context = new DelegationGrantContext(
            "tenant-1", "PASSPORT", "UNKNOWN_PERMISSION",
            VALID_EXPIRY, "tenant-1", WorkflowAssetState.ACTIVE, false, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertGrantable(context));
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    // ── passport delegatable ──

    @Test
    void failsWhenPassportNotFound() {
        DelegationGrantContext context = new DelegationGrantContext(
            "tenant-1", "PASSPORT", "RETAIL_TRANSFER_CREATE",
            VALID_EXPIRY, null, WorkflowAssetState.ACTIVE, false, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertGrantable(context));
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void failsWhenSourceTenantDoesNotOwnPassport() {
        DelegationGrantContext context = new DelegationGrantContext(
            "tenant-1", "PASSPORT", "RETAIL_TRANSFER_CREATE",
            VALID_EXPIRY, "tenant-2", WorkflowAssetState.ACTIVE, false, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertGrantable(context));
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void failsWhenPassportNotActive() {
        DelegationGrantContext context = new DelegationGrantContext(
            "tenant-1", "PASSPORT", "RETAIL_TRANSFER_CREATE",
            VALID_EXPIRY, "tenant-1", WorkflowAssetState.VOIDED, false, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertGrantable(context));
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    // ── expiry ──

    @Test
    void failsWhenExpiryInPast() {
        DelegationGrantContext context = new DelegationGrantContext(
            "tenant-1", "PASSPORT", "RETAIL_TRANSFER_CREATE",
            Instant.parse("2026-03-01T00:00:00Z"), "tenant-1", WorkflowAssetState.ACTIVE, false, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertGrantable(context));
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void failsWhenExpiryExceeds90Days() {
        DelegationGrantContext context = new DelegationGrantContext(
            "tenant-1", "PASSPORT", "RETAIL_TRANSFER_CREATE",
            Instant.parse("2026-08-01T00:00:00Z"), "tenant-1", WorkflowAssetState.ACTIVE, false, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertGrantable(context));
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    // ── duplicate ──

    @Test
    void failsWhenActiveDelegationAlreadyExists() {
        DelegationGrantContext context = new DelegationGrantContext(
            "tenant-1", "PASSPORT", "RETAIL_TRANSFER_CREATE",
            VALID_EXPIRY, "tenant-1", WorkflowAssetState.ACTIVE, true, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertGrantable(context));
        assertEquals(WorkflowErrorCode.DELEGATION_ALREADY_ACTIVE, ex.getErrorCode());
    }
}
