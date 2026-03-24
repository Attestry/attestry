package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.membership.result.MembershipPermissionTemplateResult;
import java.util.List;

public record MembershipPermissionTemplateResponse(
    String membershipId,
    String templateCode,
    String action,
    List<String> permissionCodes
) {
    public static MembershipPermissionTemplateResponse from(MembershipPermissionTemplateResult result) {
        return new MembershipPermissionTemplateResponse(
            result.membershipId(),
            result.templateCode(),
            result.action(),
            result.permissionCodes()
        );
    }
}
