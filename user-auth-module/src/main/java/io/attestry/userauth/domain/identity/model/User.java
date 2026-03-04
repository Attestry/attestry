package io.attestry.userauth.domain.identity.model;

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
