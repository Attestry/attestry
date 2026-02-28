package io.attestry.userauth.application.dto.view;

public record MembershipAdminView(
    String membershipId,
    String tenantId,
    String groupId,
    String role,
    String status
) {
}
