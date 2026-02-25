package io.attestry.userauth.security;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import org.springframework.security.core.Authentication;

public final class AuthPrincipalResolver {

    private AuthPrincipalResolver() {
    }

    public static AuthPrincipal resolve(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new DomainException(ErrorCode.ACCESS_TOKEN_INVALID, "Invalid access token");
        }
        return principal;
    }

    public static String resolveAccessToken(Authentication authentication) {
        if (authentication == null || !(authentication.getCredentials() instanceof String token)) {
            throw new DomainException(ErrorCode.ACCESS_TOKEN_INVALID, "Invalid access token");
        }
        return token;
    }
}
