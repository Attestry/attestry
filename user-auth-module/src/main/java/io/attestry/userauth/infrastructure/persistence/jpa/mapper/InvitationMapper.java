package io.attestry.userauth.infrastructure.persistence.jpa.mapper;

import io.attestry.commonlib.infrastructure.DomainMapper;
import io.attestry.userauth.domain.auth.model.Email;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.InvitationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class InvitationMapper implements DomainMapper<Invitation, InvitationJpaEntity> {

    @Override
    public Invitation toDomain(InvitationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Invitation.reconstitute(
            entity.getInvitationId(),
            entity.getTenantId(),
            Email.of(entity.getInviteeEmail()),
            entity.getRole(),
            entity.getStatus(),
            entity.getInvitedBy(),
            entity.getInvitedAt(),
            entity.getAcceptedBy(),
            entity.getAcceptedAt()
        );
    }

    @Override
    public InvitationJpaEntity toEntity(Invitation domain) {
        if (domain == null) {
            return null;
        }
        return new InvitationJpaEntity(
            domain.invitationId(),
            domain.tenantId(),
            domain.inviteeEmail().value(),
            domain.role(),
            domain.status(),
            domain.invitedBy(),
            domain.invitedAt(),
            domain.acceptedBy(),
            domain.acceptedAt()
        );
    }
}
