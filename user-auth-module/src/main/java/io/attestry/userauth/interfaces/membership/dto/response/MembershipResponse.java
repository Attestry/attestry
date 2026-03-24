package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.membership.result.MembershipResult;
import io.attestry.userauth.application.membership.view.MembershipAdminView;
import java.util.List;

public record MembershipResponse(
        String membershipId,
        String tenantId,
        String userEmail,
        List<String> roleCodes,
        String status
) {
    public static MembershipResponse from(MembershipResult membership) {
        return new MembershipResponse(
                membership.membershipId(),
                membership.tenantId(),
                membership.userEmail(),
                membership.roleCodes(),
                membership.status()
        );
    }
    public static MembershipResponse from(MembershipAdminView membership) {
        return new MembershipResponse(
                membership.membershipId(),
                membership.tenantId(),
                membership.userEmail(),
                membership.roleCodes(),
                membership.status()
        );
    }
}
