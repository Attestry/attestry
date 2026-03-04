package io.attestry.auth;

import io.attestry.userauth.application.port.AccessTokenPort;
import io.attestry.userauth.security.AuthPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.auth.token", name = "provider", havingValue = "MEMORY", matchIfMissing = true)
public class InMemoryAccessTokenService implements AccessTokenPort {

    private final Map<String, AuthPrincipal> tokenStore = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryAccessTokenService(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String issue(AuthPrincipal principal) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, principal);
        return token;
    }

    @Override
    public Optional<AuthPrincipal> parse(String token) {
        AuthPrincipal principal = tokenStore.get(token);
        if (principal == null) {
            return Optional.empty();
        }
        if (principal.expiresAt().isBefore(Instant.now(clock))) {
            tokenStore.remove(token);
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    @Override
    public void revoke(String token) {
        tokenStore.remove(token);
    }
}
