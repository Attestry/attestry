package io.attestry.userauth.application.port.auth;

import io.attestry.userauth.security.AuthPrincipal;
import java.util.Optional;

public interface AccessTokenPort {
    String issue(AuthPrincipal principal);

    Optional<AuthPrincipal> parse(String token);

    void revoke(String token);

    void revokeByUserId(String userId);
}
