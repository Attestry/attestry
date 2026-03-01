package io.attestry.workflow.application.partner.command;

import io.attestry.workflow.domain.partner.model.PartnerType;
import java.time.Instant;

public record CreatePartnerLinkCommand(
    String partnerTenantId,
    PartnerType partnerType,
    Instant proposedExpiresAt,
    String message
) {
}
