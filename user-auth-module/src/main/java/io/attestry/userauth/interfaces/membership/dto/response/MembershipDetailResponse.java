package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.membership.view.MembershipDetailView;
import java.util.List;

public record MembershipDetailResponse(
    String membershipId,
    String tenantId,
    List<String> roleCodes,
    String status,
    UserAccountResponse userAccount
) {
    public static MembershipDetailResponse from(MembershipDetailView result) {
        return new MembershipDetailResponse(
            result.membershipId(),
            result.tenantId(),
            result.roleCodes(),
            result.status(),
            new UserAccountResponse(
                result.userAccount().userId(),
                result.userAccount().email(),
                result.userAccount().phone(),
                result.userAccount().status(),
                result.userAccount().verificationLevel()
            )
        );
    }

    public record UserAccountResponse(
        String userId,
        String email,
        String phone,
        String status,
        String verificationLevel
    ) {
    }
}
