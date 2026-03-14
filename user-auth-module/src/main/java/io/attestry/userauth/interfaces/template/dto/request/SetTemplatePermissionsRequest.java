package io.attestry.userauth.interfaces.template.dto.request;

import java.util.List;

public record SetTemplatePermissionsRequest(
    List<String> permissionCodes
) {
}
