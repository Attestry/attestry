package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.partner.command.CreatePartnerLinkCommand;
import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import java.util.List;

public interface PartnerLinkUseCase {
    PartnerLinkResult create(AuthPrincipal principal, String sourceTenantId, CreatePartnerLinkCommand command);

    PartnerLinkResult approve(AuthPrincipal principal, String partnerLinkId);

    PartnerLinkResult reject(AuthPrincipal principal, String partnerLinkId, String reason);

    PartnerLinkResult suspend(AuthPrincipal principal, String partnerLinkId);

    PartnerLinkResult resume(AuthPrincipal principal, String partnerLinkId);

    PartnerLinkResult terminate(AuthPrincipal principal, String partnerLinkId, String reason);

    List<PartnerLinkResult> listByTenant(AuthPrincipal principal, String tenantId);
}
