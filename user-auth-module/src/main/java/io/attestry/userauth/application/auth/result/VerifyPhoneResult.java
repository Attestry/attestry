package io.attestry.userauth.application.auth.result;

import io.attestry.userauth.domain.auth.model.VerificationLevel;

public record VerifyPhoneResult(String userId, VerificationLevel verificationLevel) {
}
