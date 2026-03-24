package io.attestry.userauth.application.membership.view;

import java.util.List;

public record MembershipDetailView(
    String membershipId,
    String tenantId,
    List<String> roleCodes,
    String status,
    UserAccountSummary userAccount
) {
    public record UserAccountSummary(
        String userId,
        String email,
        String phone,
        String status,
        String verificationLevel
    ) {
    }
}
