package io.attestry.userauth.interfaces.auth;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;

public final class BearerTokenExtractor {

    private static final String PREFIX = "Bearer ";

    private BearerTokenExtractor() {
    }

    public static String extract(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) {
            throw new DomainException(ErrorCode.ACCESS_TOKEN_INVALID, "Bearer token is required");
        }
        return authorizationHeader.substring(PREFIX.length()).trim();
    }
}
