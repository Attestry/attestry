package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.dto.result.TenantAvailableTemplateCodesResult;
import java.util.List;

public record TenantAvailableTemplateCodesResponse(
    String tenantId,
    List<String> templateCodes
) {
    public static TenantAvailableTemplateCodesResponse from(TenantAvailableTemplateCodesResult result) {
        return new TenantAvailableTemplateCodesResponse(
            result.tenantId(),
            result.templateCodes()
        );
    }
}
