package io.attestry.userauth.application.auth.internal;

import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.application.port.membership.MembershipProjectionPort;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.policy.MembershipSelectionPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEffectiveScopeResolver {

    private final MembershipPort membershipPort;
    private final MembershipProjectionPort membershipProjectionPort;

    public Optional<Membership> resolveActiveMembership(String userId, String tenantId) {
        Optional<Membership> requestedMembership = (tenantId != null)
            ? membershipPort.findByUserIdAndTenantId(userId, tenantId)
            : Optional.empty();
        List<Membership> memberships = membershipPort.findByUserId(userId);
        return MembershipSelectionPolicy.resolve(tenantId, requestedMembership, memberships);
    }

    public Set<String> resolveEffectiveScopes(String userId, String tenantId) {
        return resolveScopes(resolveActiveMembership(userId, tenantId), false);
    }

    public Set<String> resolveLoginScopes(Optional<Membership> membership) {
        return resolveScopes(membership, true);
    }

    private Set<String> resolveScopes(Optional<Membership> membership, boolean requireOwnerPermissions) {
        Set<String> scopes = resolveOwnerPermissions(requireOwnerPermissions);
        membership.ifPresent(m -> scopes.addAll(resolveMembershipScopes(m)));
        return scopes;
    }

    private Set<String> resolveMembershipScopes(Membership membership) {
        Set<String> permissionCodes = membershipProjectionPort
            .findPermissionCodesByMembershipId(membership.membershipId());
        return Set.copyOf(permissionCodes);
    }

    private Set<String> resolveOwnerPermissions(boolean required) {
        Set<String> permissionCodes = membershipProjectionPort
            .findPermissionCodesByGlobalRoleCode(RoleCodes.OWNER_DEFAULT);
        if (required && permissionCodes.isEmpty()) {
            throw new IllegalStateException("OWNER_DEFAULT role permissions are not configured");
        }
        return new HashSet<>(permissionCodes);
    }
}
