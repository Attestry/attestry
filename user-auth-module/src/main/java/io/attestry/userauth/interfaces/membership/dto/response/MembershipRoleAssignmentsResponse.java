package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import java.util.List;

public record MembershipRoleAssignmentsResponse(
    String membershipId,
    List<String> roleCodes
) {
    public static MembershipRoleAssignmentsResponse from(MembershipRoleAssignmentsResult result) {
        return new MembershipRoleAssignmentsResponse(result.membershipId(), result.roleCodes());
    }
}
