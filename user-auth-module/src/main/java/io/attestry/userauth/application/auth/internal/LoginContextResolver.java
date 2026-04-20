package io.attestry.userauth.application.auth.internal;

import io.attestry.userauth.domain.authorization.model.LoginContext;
import io.attestry.userauth.domain.membership.model.Membership;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Resolves the context (tenant + scopes) for token during login
@Component
@RequiredArgsConstructor
public class LoginContextResolver {

    private final UserEffectiveScopeResolver userEffectiveScopeResolver;

    public LoginContext resolve(String userId, String tenantId) {
        Optional<Membership> activeMembership = userEffectiveScopeResolver.resolveActiveMembership(userId, tenantId);
        Set<String> scopes = userEffectiveScopeResolver.resolveLoginScopes(activeMembership);
        return activeMembership
            .map(membership -> LoginContext.withMembership(membership, scopes))
            .orElseGet(() -> LoginContext.owner(scopes));
    }
}
