package io.attestry.userauth.application.membership.result;

import java.util.List;

public record MembershipPermissionTemplateResult(
    String membershipId,
    String templateCode,
    String action,
    List<String> permissionCodes
) {
}
