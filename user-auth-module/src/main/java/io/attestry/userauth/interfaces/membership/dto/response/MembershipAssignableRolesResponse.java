package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import java.util.List;

public record MembershipAssignableRolesResponse(String membershipId, List<String> roleCodes) {

    public static MembershipAssignableRolesResponse from(MembershipAssignableRolesResult result) {
        return new MembershipAssignableRolesResponse(result.membershipId(), result.roleCodes());
    }
}
