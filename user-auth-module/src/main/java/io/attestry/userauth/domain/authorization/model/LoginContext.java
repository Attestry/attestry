package io.attestry.userauth.domain.authorization.model;

import io.attestry.userauth.domain.membership.model.Membership;
import java.util.Set;

public record LoginContext(
    String tenantId,
    Set<String> scopes
) {
    public static LoginContext owner(Set<String> scopes) {
        return new LoginContext(null, Set.copyOf(scopes));
    }

    public static LoginContext withMembership(Membership membership, Set<String> scopes) {
        return new LoginContext(membership.tenantId(), Set.copyOf(scopes));
    }
}
