package io.attestry.userauth.application.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.dto.view.MembershipView;
import io.attestry.userauth.application.port.MembershipPermissionQueryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.organization.model.TenantType;
import io.attestry.userauth.domain.organization.repository.TenantRepository;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MembershipQueryServiceTest {

    @Test
    void shouldMapMembershipsToViewWithEffectiveScopes() {
        MembershipRepositoryPort repository = new MembershipRepositoryPort() {
            @Override
            public List<Membership> findByUserId(String userId) {
                return List.of(Membership.reconstitute(
                    "m1", userId, "t1",
                    TenantType.RETAIL, MembershipRole.OPERATOR, MembershipStatus.ACTIVE,
                    TenantStatus.ACTIVE, Set.of()
                ));
            }

            @Override
            public Optional<Membership> findByUserIdAndTenantId(String userId, String tenantId) {
                return Optional.empty();
            }
        };

        MembershipPermissionQueryPort permissionQueryPort = new MembershipPermissionQueryPort() {
            @Override
            public Set<String> findPermissionCodesByMembershipId(String membershipId) {
                return Set.of(PermissionCodes.RETAIL_TRANSFER_CREATE);
            }

            @Override
            public Set<String> findPermissionCodesByGlobalRoleCode(String roleCode) {
                return Set.of();
            }

            @Override
            public Set<String> findRoleCodesByMembershipId(String membershipId) {
                return Set.of("TENANT_OPERATOR");
            }
        };

        TenantRepository tenantRepository = new TenantRepository() {
            @Override
            public io.attestry.userauth.domain.organization.model.Tenant save(io.attestry.userauth.domain.organization.model.Tenant tenant) {
                return tenant;
            }

            @Override
            public Optional<io.attestry.userauth.domain.organization.model.Tenant> findById(String tenantId) {
                return Optional.empty();
            }
        };

        MembershipQueryService service = new MembershipQueryService(repository, permissionQueryPort, tenantRepository);
        List<MembershipView> views = service.getMemberships("u1");

        assertEquals(1, views.size());
        MembershipView view = views.getFirst();
        assertEquals("m1", view.membershipId());
        assertEquals(List.of("TENANT_OPERATOR"), view.roleCodes());
        assertTrue(view.effectiveScopes().contains(PermissionCodes.RETAIL_TRANSFER_CREATE));
    }
}
