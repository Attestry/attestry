package io.attestry.userauth.application.membership.result;

import java.util.List;

public record MembershipRoleAssignmentsResult(
    String membershipId,
    List<String> roleCodes
) {
}
