package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.membership.view.MembershipAssignableRolesView;
import java.util.List;

public record MembershipAssignableRolesResponse(String membershipId, List<String> roleCodes) {

    public static MembershipAssignableRolesResponse from(MembershipAssignableRolesView result) {
        return new MembershipAssignableRolesResponse(result.membershipId(), result.roleCodes());
    }
}
