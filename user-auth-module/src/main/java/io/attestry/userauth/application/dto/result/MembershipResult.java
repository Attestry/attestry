package io.attestry.userauth.application.dto.result;

public record MembershipResult(
    String membershipId,
    String tenantId,
    String groupId,
    String role,
    String status
) {
}
