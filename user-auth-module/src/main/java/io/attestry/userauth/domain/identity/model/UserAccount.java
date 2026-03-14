package io.attestry.userauth.domain.identity.model;

import io.attestry.commonlib.domain.AggregateRoot;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.identity.policy.PasswordMatcher;

import java.util.UUID;

public class UserAccount extends AggregateRoot {

    private final String userId;
    private final Email email;
    private String phone;
    private String passwordHash;
    private UserStatus status;
    private VerificationLevel verificationLevel;

    private UserAccount(String userId, Email email, String phone, String passwordHash,
                        UserStatus status, VerificationLevel verificationLevel) {
        this.userId = userId;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.status = status;
        this.verificationLevel = verificationLevel;
    }

    public static UserAccount register(String email, String phone, String passwordHash) {
        Email normalizedEmail = Email.of(email);
        validateRegister(passwordHash);
        return new UserAccount(
            UUID.randomUUID().toString(),
            normalizedEmail,
            phone,
            passwordHash,
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        );
    }

    public static UserAccount reconstitute(String userId, Email email, String phone,
                                            String passwordHash, UserStatus status,
                                            VerificationLevel verificationLevel) {
        return new UserAccount(userId, email, phone, passwordHash, status, verificationLevel);
    }

    private static void validateRegister(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_CREDENTIALS, "Password hash is required");
        }
    }

    public void updatePhone(String phone) {
        if (phone == null) {
            this.phone = null;
            return;
        }

        String normalizedPhone = phone.trim();
        this.phone = normalizedPhone.isBlank() ? null : normalizedPhone;
    }

    public void changePassword(String currentRawPassword, String newPasswordHash, PasswordMatcher matcher) {
        if (currentRawPassword == null || currentRawPassword.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_CREDENTIALS, "Current password is required to change password");
        }
        assertPasswordMatches(currentRawPassword, matcher);
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_CREDENTIALS, "New password hash is required");
        }
        this.passwordHash = newPasswordHash;
    }

    public void updateProfile(String phone, String currentRawPassword,
                              String newPasswordHash, PasswordMatcher matcher) {
        checkActiveStatus();
        if (phone != null) {
            updatePhone(phone);
        }
        if (newPasswordHash != null) {
            changePassword(currentRawPassword, newPasswordHash, matcher);
        }
    }

    public void verifyPhone() {
        this.verificationLevel = VerificationLevel.PHONE_VERIFIED;
    }

    public void checkActiveStatus() {
        if (status != UserStatus.ACTIVE) throw new UserAuthDomainException(UserAuthErrorCode.USER_SUSPENDED, "User is suspended");

    }

    public void assertPasswordMatches(String rawPassword, PasswordMatcher matcher) {
        if (!matcher.matches(rawPassword, passwordHash)) throw new UserAuthDomainException(UserAuthErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    // Getters
    public String userId() { return userId; }
    public Email email() { return email; }
    public String phone() { return phone; }
    public String passwordHash() { return passwordHash; }
    public UserStatus status() { return status; }
    public VerificationLevel verificationLevel() { return verificationLevel; }
}
