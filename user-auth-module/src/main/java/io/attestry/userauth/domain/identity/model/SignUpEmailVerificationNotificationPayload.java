package io.attestry.userauth.domain.identity.model;

public record SignUpEmailVerificationNotificationPayload(
    String verificationId,
    String email,
    String code,
    long expiresInSeconds
) {
}
