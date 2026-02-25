package io.attestry.userauth.domain.user.vo;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import java.util.Locale;

public record Email(String value) {

    public static Email of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS, "Email is required");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains("@")) {
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS, "Invalid email format");
        }
        return new Email(normalized);
    }
}
