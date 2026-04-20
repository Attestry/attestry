package io.attestry.userauth.application.auth.command;

import java.time.Instant;

public record SignUpEmailVerificationResult(
    String verificationId,
    String email,
    Instant expiresAt,
    Instant verifiedAt,
    int resendCount,
    int resendLimit,
    long resendCooldownSeconds
) {
}
