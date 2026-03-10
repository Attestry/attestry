package io.attestry.userauth.infrastructure.persistence.jpa.mapper;

import io.attestry.commonlib.infrastructure.DomainMapper;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipJpaEntity;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MembershipMapper implements DomainMapper<Membership, MembershipJpaEntity> {

    @Override
    public Membership toDomain(MembershipJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Membership.reconstitute(
            entity.getMembershipId(),
            entity.getUserId(),
            entity.getTenantId(),
            entity.getTenantType(),
            entity.getRole(),
            entity.getStatus(),
            entity.getTenantStatus(),
            Set.of()
        );
    }

    @Override
    public MembershipJpaEntity toEntity(Membership domain) {
        if (domain == null) {
            return null;
        }
        return new MembershipJpaEntity(
            domain.membershipId(),
            domain.userId(),
            domain.tenantId(),
            domain.groupType(),
            domain.role(),
            domain.status(),
            domain.tenantStatus()
        );
    }
}
