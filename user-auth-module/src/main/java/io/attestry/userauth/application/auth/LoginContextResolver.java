package io.attestry.userauth.application.auth;

import io.attestry.userauth.domain.authorization.model.LoginContext;
import io.attestry.userauth.domain.membership.model.Membership;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// LoginContextResolver는 로그인 시 토큰에 넣을 컨텍스트(tenant + scopes)를 계산하는 역할
@Component
@RequiredArgsConstructor
public class LoginContextResolver {

    private final UserEffectiveScopeResolver userEffectiveScopeResolver;

    public LoginContext resolve(String userId, String tenantId) {
        Optional<Membership> activeMembership = userEffectiveScopeResolver.resolveActiveMembership(userId, tenantId);
        Set<String> scopes = userEffectiveScopeResolver.resolveLoginScopes(activeMembership);
        return activeMembership
            .map(membership -> {
                return LoginContext.withMembership(membership, scopes);
            })
            .orElseGet(() -> LoginContext.owner(scopes));
    }
}
