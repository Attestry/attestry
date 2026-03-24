package io.attestry.userauth.application.membership.view;

import java.util.List;

public record MembershipRoleAssignmentsView(
    String membershipId,
    List<String> roleCodes
) {
}
