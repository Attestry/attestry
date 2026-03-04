package io.attestry.userauth.application.port;

import io.attestry.userauth.security.AuthPrincipal;
import java.util.Optional;

public interface AccessTokenPort {
    String issue(AuthPrincipal principal);

    Optional<AuthPrincipal> parse(String token);

    void revoke(String token);
}
