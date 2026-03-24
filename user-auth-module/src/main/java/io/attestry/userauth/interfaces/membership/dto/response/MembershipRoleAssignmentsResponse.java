package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.membership.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.membership.view.MembershipRoleAssignmentsView;
import java.util.List;

public record MembershipRoleAssignmentsResponse(
    String membershipId,
    List<String> roleCodes
) {
    public static MembershipRoleAssignmentsResponse from(MembershipRoleAssignmentsView result) {
        return new MembershipRoleAssignmentsResponse(result.membershipId(), result.roleCodes());
    }

    public static MembershipRoleAssignmentsResponse from(MembershipRoleAssignmentsResult result) {
        return new MembershipRoleAssignmentsResponse(result.membershipId(), result.roleCodes());
    }
}
