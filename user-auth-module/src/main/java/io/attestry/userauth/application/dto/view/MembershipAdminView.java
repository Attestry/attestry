package io.attestry.userauth.application.dto.view;

import java.util.List;

public record MembershipAdminView(
    String membershipId,
    String tenantId,
    String groupId,
    List<String> roleCodes,
    String status
) {
}
