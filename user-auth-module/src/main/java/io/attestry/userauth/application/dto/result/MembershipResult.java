package io.attestry.userauth.application.dto.result;

import java.util.List;

public record MembershipResult(
    String membershipId,
    String tenantId,
    String groupId,
    List<String> roleCodes,
    String status
) {
}
