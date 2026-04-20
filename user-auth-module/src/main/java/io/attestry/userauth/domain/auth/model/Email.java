package io.attestry.userauth.domain.auth.model;

import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {
    public static final String VALIDATION_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,24}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(VALIDATION_PATTERN);

    public static Email of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_CREDENTIALS, "Email is required");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_CREDENTIALS, "Invalid email format");
        }
        return new Email(normalized);
    }
}
