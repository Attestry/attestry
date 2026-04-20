package io.attestry.workflow.domain.partner.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.partner.policy.PartnerLinkCreatePolicy.PartnerLinkCreateContext;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PartnerLinkCreatePolicyTest {

    private final PartnerLinkCreatePolicy policy = new PartnerLinkCreatePolicy();

    private static final Instant NOW = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant VALID_EXPIRY = Instant.parse("2026-05-01T00:00:00Z");

    private PartnerLinkCreateContext validContext() {
        return new PartnerLinkCreateContext(
            "source-tenant", "target-tenant", true, true, false, VALID_EXPIRY, NOW
        );
    }

    // ── success ──

    @Test
    void creatable_success() {
        assertDoesNotThrow(() -> policy.assertCreatable(validContext()));
    }

    @Test
    void creatable_nullExpiry_success() {
        PartnerLinkCreateContext context = new PartnerLinkCreateContext(
            "source-tenant", "target-tenant", true, true, false, null, NOW
        );
        assertDoesNotThrow(() -> policy.assertCreatable(context));
    }

    // ── tenant validation ──

    @Test
    void failsWhenSameTenant() {
        PartnerLinkCreateContext context = new PartnerLinkCreateContext(
            "same-tenant", "same-tenant", true, true, false, VALID_EXPIRY, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertCreatable(context));
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void failsWhenSourceTenantInactive() {
        PartnerLinkCreateContext context = new PartnerLinkCreateContext(
            "source-tenant", "target-tenant", false, true, false, VALID_EXPIRY, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertCreatable(context));
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void failsWhenTargetTenantInactive() {
        PartnerLinkCreateContext context = new PartnerLinkCreateContext(
            "source-tenant", "target-tenant", true, false, false, VALID_EXPIRY, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertCreatable(context));
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    // ── duplicate ──

    @Test
    void failsWhenActiveDuplicateExists() {
        PartnerLinkCreateContext context = new PartnerLinkCreateContext(
            "source-tenant", "target-tenant", true, true, true, VALID_EXPIRY, NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertCreatable(context));
        assertEquals(WorkflowErrorCode.PARTNER_LINK_ALREADY_ACTIVE, ex.getErrorCode());
    }

    // ── expiry ──

    @Test
    void failsWhenExpiryInPast() {
        PartnerLinkCreateContext context = new PartnerLinkCreateContext(
            "source-tenant", "target-tenant", true, true, false,
            Instant.parse("2026-03-01T00:00:00Z"), NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertCreatable(context));
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void failsWhenExpiryExceeds90Days() {
        PartnerLinkCreateContext context = new PartnerLinkCreateContext(
            "source-tenant", "target-tenant", true, true, false,
            Instant.parse("2026-08-01T00:00:00Z"), NOW
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class,
            () -> policy.assertCreatable(context));
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }
}
