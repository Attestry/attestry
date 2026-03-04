package io.attestry.workflow.application.delegation;

import io.attestry.workflow.domain.partner.model.PartnerLink;

public interface RelationshipValidator {
    PartnerLink assertEligible(String partnerLinkId, String sourceTenantId, String targetTenantId);

    PartnerLink assertEligibleBySource(String partnerLinkId, String sourceTenantId);
}

