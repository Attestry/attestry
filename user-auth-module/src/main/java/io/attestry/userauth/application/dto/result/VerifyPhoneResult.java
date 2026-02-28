package io.attestry.userauth.application.dto.result;

import io.attestry.userauth.domain.user.enums.VerificationLevel;

public record VerifyPhoneResult(String userId, VerificationLevel verificationLevel) {
}
