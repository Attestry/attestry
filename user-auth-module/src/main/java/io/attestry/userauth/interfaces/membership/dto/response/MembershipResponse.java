package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.view.MembershipAdminView;

public record MembershipResponse(
        String membershipId,
        String tenantId,
        String groupId,
        String role,
        String status
) {
    public static MembershipResponse from(MembershipResult membership) {
        return new MembershipResponse(
                membership.membershipId(),
                membership.tenantId(),
                membership.groupId(),
                membership.role(),
                membership.status()
        );
    }
    public static MembershipResponse from(MembershipAdminView membership) {
        return new MembershipResponse(
                membership.membershipId(),
                membership.tenantId(),
                membership.groupId(),
                membership.role(),
                membership.status()
        );
    }
}