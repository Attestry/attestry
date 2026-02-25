package io.attestry.userauth.domain.user.model;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.user.enums.UserStatus;
import io.attestry.userauth.domain.user.enums.VerificationLevel;
import io.attestry.userauth.domain.user.vo.Email;

import java.util.UUID;


// aggregateRoot
public record UserAccount(User user, String passwordHash) {

    public static UserAccount register(String email, String phone, String passwordHash) {
        Email normalizedEmail = Email.of(email);
        validateRegister(passwordHash);
        User user = new User(
            UUID.randomUUID().toString(),
            normalizedEmail,
            phone,
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        );
        return new UserAccount(user, passwordHash);
    }

    private static void validateRegister(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS, "Password hash is required");
        }
    }

    public UserAccount withVerificationLevel(VerificationLevel level) {
        return new UserAccount(
            new User(
                user.userId(),
                user.email(),
                user.phone(),
                user.status(),
                level
            ),
            passwordHash
        );
    }
}
