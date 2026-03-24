package io.attestry.userauth.application.membership.view;

import java.util.List;

public record TenantAvailableTemplateCodesView(
    String tenantId,
    List<String> templateCodes
) {
}
