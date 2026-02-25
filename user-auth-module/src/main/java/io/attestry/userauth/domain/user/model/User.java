package io.attestry.userauth.domain.user.model;

import io.attestry.userauth.domain.user.enums.UserStatus;
import io.attestry.userauth.domain.user.enums.VerificationLevel;
import io.attestry.userauth.domain.user.vo.Email;

public record User(
    String userId,
    Email email,
    String phone,
    UserStatus status,
    VerificationLevel verificationLevel
) {
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
