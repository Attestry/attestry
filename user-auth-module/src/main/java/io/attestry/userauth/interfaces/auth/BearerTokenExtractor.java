package io.attestry.userauth.interfaces.auth;

import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
public final class BearerTokenExtractor {

    private static final String PREFIX = "Bearer ";

    private BearerTokenExtractor() {
    }

    public static String extract(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) {
            throw new UserAuthDomainException(UserAuthErrorCode.ACCESS_TOKEN_INVALID, "Bearer token is required");
        }
        return authorizationHeader.substring(PREFIX.length()).trim();
    }
}
