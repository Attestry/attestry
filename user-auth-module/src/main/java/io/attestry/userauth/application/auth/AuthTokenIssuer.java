package io.attestry.userauth.application.auth;

import io.attestry.userauth.application.dto.result.AuthTokenResult;
import io.attestry.userauth.application.port.auth.AccessTokenPort;
import io.attestry.userauth.domain.authorization.model.LoginContext;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.security.AuthPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthTokenIssuer {

    private final AccessTokenPort accessTokenPort;
    private final Clock clock;

    @Value("${app.auth.token.access-ttl:PT15M}")
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    public AuthTokenResult issue(UserAccount account, LoginContext loginContext) {
        Instant now = Instant.now(clock);
        AuthPrincipal principal = AuthPrincipal.issue(
            account.userId(),
            loginContext.tenantId(),
            account.verificationLevel(),
            loginContext.scopes(),
            now,
            accessTokenTtl
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
