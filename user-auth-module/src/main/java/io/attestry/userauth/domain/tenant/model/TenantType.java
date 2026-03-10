package io.attestry.userauth.domain.tenant.model;

import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import java.util.Locale;

public enum TenantType {
    BRAND,
    RETAIL,
    SERVICE,
    INTERNAL;

    public static TenantType parseSupported(String value) {
        if (value == null || value.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST, "type is required");
        }

        try {
            TenantType type = TenantType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (type == BRAND || type == RETAIL || type == SERVICE) {
                return type;
            }
        } catch (IllegalArgumentException ignored) {
            // handled below
        }

        throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST,
            "type must be BRAND, RETAIL, or SERVICE");
    }

    public static TenantType parseSupportedOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseSupported(value);
    }
}
