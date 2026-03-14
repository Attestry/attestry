package io.attestry.userauth.application.dto.view;

import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.domain.identity.model.UserStatus;
import io.attestry.userauth.domain.identity.model.VerificationLevel;

public record MyAccountView(
    String userId,
    String email,
    String phone,
    UserStatus status,
    VerificationLevel verificationLevel
) {

    public static MyAccountView from(UserAccount account) {
        return new MyAccountView(
            account.userId(),
            account.email().value(),
            account.phone(),
            account.status(),
            account.verificationLevel()
        );
    }
}
