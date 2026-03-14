package io.attestry.userauth.application.dto.result;

import java.util.List;

public record MembershipAssignableRolesResult(String membershipId, List<String> roleCodes) {
}
