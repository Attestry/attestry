package io.attestry.userauth.application.dto.result;

import io.attestry.userauth.domain.identity.model.VerificationLevel;

public record VerifyPhoneResult(String userId, VerificationLevel verificationLevel) {
}
