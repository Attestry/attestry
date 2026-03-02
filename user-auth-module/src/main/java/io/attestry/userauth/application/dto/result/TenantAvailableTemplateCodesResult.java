package io.attestry.userauth.application.dto.result;

import java.util.List;

public record TenantAvailableTemplateCodesResult(
    String tenantId,
    List<String> templateCodes
) {
}
