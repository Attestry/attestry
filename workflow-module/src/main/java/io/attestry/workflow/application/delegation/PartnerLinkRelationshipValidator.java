package io.attestry.workflow.application.delegation;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.repository.PartnerLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartnerLinkRelationshipValidator implements RelationshipValidator {

    private final PartnerLinkRepository partnerLinkRepository;


    @Override
    public PartnerLink assertEligibleBySource(String partnerLinkId, String sourceTenantId) {
        PartnerLink partnerLink = partnerLinkRepository.findById(partnerLinkId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.PARTNER_LINK_NOT_FOUND, "Partner link not found"));

        if (partnerLink.status() != PartnerLinkStatus.ACTIVE) {
            throw new WorkflowDomainException(WorkflowErrorCode.PARTNER_LINK_INVALID_STATE, "Partner link must be active");
        }
        if (!partnerLink.sourceTenantId().equals(sourceTenantId)) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Partner link tenant mismatch");
        }
        return partnerLink;
    }
}

