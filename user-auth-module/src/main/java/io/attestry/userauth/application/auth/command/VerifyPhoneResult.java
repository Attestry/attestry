package io.attestry.userauth.application.auth.command;

import io.attestry.userauth.domain.auth.model.VerificationLevel;

public record VerifyPhoneResult(String userId, VerificationLevel verificationLevel) {
}
