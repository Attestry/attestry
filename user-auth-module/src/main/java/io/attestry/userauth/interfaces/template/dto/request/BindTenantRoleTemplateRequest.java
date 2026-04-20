package io.attestry.userauth.interfaces.template.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BindTenantRoleTemplateRequest(
    @NotBlank(message = "Role code is required")
    String roleCode,
    @NotBlank(message = "Template code is required")
    String templateCode
) {
}
