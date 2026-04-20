package io.attestry.userauth.interfaces.template.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SetTemplatePermissionsRequest(
    @NotEmpty(message = "Permission codes must not be empty")
    List<String> permissionCodes
) {
}
