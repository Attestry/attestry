package io.attestry.userauth.application.auth.query;

import io.attestry.userauth.domain.auth.model.UserAccount;
import io.attestry.userauth.domain.auth.model.UserStatus;
import io.attestry.userauth.domain.auth.model.VerificationLevel;

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
