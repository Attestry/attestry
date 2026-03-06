package io.attestry.userauth.application.dto.view;

import java.util.List;

public record MembershipAdminView(
    String membershipId,
    String tenantId,
    String userEmail,
    List<String> roleCodes,
    String status
) {
}
