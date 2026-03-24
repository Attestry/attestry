package io.attestry.workflow.application.delegation.support;

import io.attestry.workflow.domain.partner.model.PartnerLink;

public interface RelationshipValidator {
    PartnerLink assertEligibleBySource(String partnerLinkId, String sourceTenantId);
}
