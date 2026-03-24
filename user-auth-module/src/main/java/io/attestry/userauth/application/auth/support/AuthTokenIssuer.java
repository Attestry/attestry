package io.attestry.userauth.application.auth.support;

import io.attestry.userauth.application.auth.result.AuthTokenResult;
import io.attestry.userauth.application.port.auth.AccessTokenPort;
import io.attestry.userauth.domain.authorization.model.LoginContext;
import io.attestry.userauth.domain.auth.model.UserAccount;
import io.attestry.userauth.infrastructure.config.AuthTokenProperties;
import io.attestry.userauth.security.AuthPrincipal;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthTokenIssuer {

    private final AccessTokenPort accessTokenPort;
    private final Clock clock;
    private final AuthTokenProperties authTokenProperties;

    public AuthTokenResult issue(UserAccount account, LoginContext loginContext) {
        Instant now = Instant.now(clock);
        AuthPrincipal principal = AuthPrincipal.issue(
            account.userId(),
            loginContext.tenantId(),
            account.verificationLevel(),
            loginContext.scopes(),
            now,
            authTokenProperties.getAccessTtl()
        );
        String token = accessTokenPort.issue(principal);

        return new AuthTokenResult(
            token,
            "Bearer",
            principal.expiresAt(),
            account.userId(),
            loginContext.tenantId()
        );
    }
}
