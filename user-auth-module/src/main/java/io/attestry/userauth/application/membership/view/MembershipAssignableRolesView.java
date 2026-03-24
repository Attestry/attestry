package io.attestry.userauth.application.membership.view;

import java.util.List;

public record MembershipAssignableRolesView(String membershipId, List<String> roleCodes) {
}
