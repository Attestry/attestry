package io.attestry.userauth.interfaces.auth.dto.response;

import io.attestry.userauth.application.dto.result.SignUpEmailVerificationResult;
import java.time.Instant;

public record SignUpEmailVerificationResponse(
    String verificationId,
    String email,
    Instant expiresAt,
    Instant verifiedAt,
    int resendCount,
    int resendLimit,
    long resendCooldownSeconds
) {
    public static SignUpEmailVerificationResponse from(SignUpEmailVerificationResult result) {
        return new SignUpEmailVerificationResponse(
            result.verificationId(),
            result.email(),
            result.expiresAt(),
            result.verifiedAt(),
            result.resendCount(),
            result.resendLimit(),
            result.resendCooldownSeconds()
        );
    }
}
