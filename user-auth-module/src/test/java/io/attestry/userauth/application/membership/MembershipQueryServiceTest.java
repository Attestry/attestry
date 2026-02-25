package io.attestry.userauth.application.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.dto.MembershipView;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.auth.model.Scope;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MembershipQueryServiceTest {

    @Test
    void shouldMapMembershipsToViewWithEffectiveScopes() {
        MembershipRepositoryPort repository = new MembershipRepositoryPort() {
            @Override
            public List<Membership> findByUserId(String userId) {
                return List.of(new Membership(
                    "m1",
                    userId,
                    "g1",
                    "t1",
                    GroupType.RETAIL,
                    MembershipRole.OPERATOR,
                    MembershipStatus.ACTIVE,
                    GroupStatus.ACTIVE,
                    TenantStatus.ACTIVE
                ));
            }

            @Override
            public Optional<Membership> findByUserIdAndContext(String userId, String tenantId, String groupId) {
                return Optional.empty();
            }
        };

        MembershipQueryService service = new MembershipQueryService(repository);
        List<MembershipView> views = service.getMemberships("u1");

        assertEquals(1, views.size());
        MembershipView view = views.getFirst();
        assertEquals("m1", view.membershipId());
        assertTrue(view.effectiveScopes().contains(Scope.RETAIL_RELEASE));
        assertTrue(view.effectiveScopes().contains(Scope.RETAIL_TRANSFER_CREATE));
    }
}
