package io.attestry.userauth.domain.identity.model;

import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;import java.util.Locale;

public record Email(String value) {

    public static Email of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_CREDENTIALS, "Email is required");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains("@")) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_CREDENTIALS, "Invalid email format");
        }
        return new Email(normalized);
    }
}
