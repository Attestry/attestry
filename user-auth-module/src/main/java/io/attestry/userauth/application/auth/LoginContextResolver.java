package io.attestry.userauth.application.auth;

import io.attestry.userauth.application.port.MembershipPort;
import io.attestry.userauth.application.port.MembershipProjectionPort;
import io.attestry.userauth.domain.authorization.model.LoginContext;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.policy.MembershipSelectionPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// LoginContextResolver는 로그인 시 토큰에 넣을 컨텍스트(tenant + scopes)를 계산하는 역할
@Component
@RequiredArgsConstructor
public class LoginContextResolver {

    private final MembershipPort membershipPort;
    private final MembershipProjectionPort membershipProjectionPort;

    public LoginContext resolve(String userId, String tenantId) {
        Set<String> scopes = resolveOwnerPermissions();
        return resolveActiveMembership(userId, tenantId)
            .map(activeMembership -> {
                scopes.addAll(resolveMembershipScopes(activeMembership));
                return LoginContext.withMembership(activeMembership, scopes);
            })
            .orElseGet(() -> LoginContext.owner(scopes));
    }

    private Optional<Membership> resolveActiveMembership(String userId, String tenantId) {
        Optional<Membership> requestedMembership = (tenantId != null)
            ? membershipPort.findByUserIdAndTenantId(userId, tenantId)
            : Optional.empty();
        List<Membership> memberships = membershipPort.findByUserId(userId);
        return MembershipSelectionPolicy.resolve(tenantId, requestedMembership, memberships);
    }

    private Set<String> resolveMembershipScopes(Membership membership) {
        Set<String> permissionCodes = membershipProjectionPort
            .findPermissionCodesByMembershipId(membership.membershipId());
        return Set.copyOf(permissionCodes);
    }

    private Set<String> resolveOwnerPermissions() {
        Set<String> permissionCodes = membershipProjectionPort
            .findPermissionCodesByGlobalRoleCode(RoleCodes.OWNER_DEFAULT);
        if (permissionCodes.isEmpty()) {
            throw new IllegalStateException("OWNER_DEFAULT role permissions are not configured");
        }
        return new HashSet<>(permissionCodes);
    }
}
