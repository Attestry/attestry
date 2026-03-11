package io.attestry.workflow.domain.partner.policy;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class PartnerLinkCreatePolicy {

    private static final Duration MAX_EXPIRY_WINDOW = Duration.ofDays(90);

    public void assertCreatable(PartnerLinkCreateContext context) {
        assertDifferentTenants(context.sourceTenantId(), context.targetTenantId());
        assertActiveTenants(context.sourceTenantActive(), context.targetTenantActive());
        assertNoActiveDuplicate(context.activeDuplicateExists());
        assertExpiryValid(context.proposedExpiresAt(), context.now());
    }

    private void assertDifferentTenants(String sourceTenantId, String targetTenantId) {
        if (sourceTenantId.equals(targetTenantId)) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "Target tenant must be different from source tenant"
            );
        }
    }

    private void assertActiveTenants(boolean sourceTenantActive, boolean targetTenantActive) {
        if (!sourceTenantActive || !targetTenantActive) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "Tenant not found or inactive"
            );
        }
    }

    private void assertNoActiveDuplicate(boolean activeDuplicateExists) {
        if (activeDuplicateExists) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.PARTNER_LINK_ALREADY_ACTIVE,
                "Active partner link already exists"
            );
        }
    }

    private void assertExpiryValid(Instant proposedExpiresAt, Instant now) {
        if (proposedExpiresAt == null) {
            return;
        }
        if (!proposedExpiresAt.isAfter(now)) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "proposedExpiresAt must be in the future"
            );
        }
        if (proposedExpiresAt.isAfter(now.plus(MAX_EXPIRY_WINDOW))) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "proposedExpiresAt exceeds max 90 days"
            );
        }
    }

    public record PartnerLinkCreateContext(
        String sourceTenantId,
        String targetTenantId,
        boolean sourceTenantActive,
        boolean targetTenantActive,
        boolean activeDuplicateExists,
        Instant proposedExpiresAt,
        Instant now
    ) {
    }
}
