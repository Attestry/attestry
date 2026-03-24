package io.attestry.userauth.domain.auth.model;

public record SignUpEmailVerificationNotificationPayload(
    String verificationId,
    String email,
    String code,
    long expiresInSeconds
) {
}
