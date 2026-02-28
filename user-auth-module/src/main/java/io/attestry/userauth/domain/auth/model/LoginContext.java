package io.attestry.userauth.domain.auth.model;

import io.attestry.userauth.domain.membership.model.Membership;
import java.util.Set;

public record LoginContext(
    String tenantId,
    String groupId,
    Set<String> scopes
) {
    public static LoginContext owner(Set<String> scopes) {
        return new LoginContext(null, null, Set.copyOf(scopes));
    }

    public static LoginContext withMembership(Membership membership, Set<String> scopes) {
        return new LoginContext(membership.tenantId(), membership.groupId(), Set.copyOf(scopes));
    }
}
