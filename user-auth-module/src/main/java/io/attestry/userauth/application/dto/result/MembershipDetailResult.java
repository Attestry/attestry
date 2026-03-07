package io.attestry.userauth.application.dto.result;

import java.util.List;

public record MembershipDetailResult(
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

