package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.domain.identity.model.Email;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.repository.InvitationRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.InvitationJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.InvitationJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInvitationRepositoryAdapter implements InvitationRepository {

    private final InvitationJpaRepository repository;

    public JpaInvitationRepositoryAdapter(InvitationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Invitation save(Invitation invitation) {
        InvitationJpaEntity saved = repository.save(new InvitationJpaEntity(
            invitation.invitationId(),
            invitation.tenantId(),
            invitation.groupId(),
            invitation.inviteeEmail().value(),
            invitation.role(),
            invitation.status(),
            invitation.invitedBy(),
            invitation.invitedAt(),
            invitation.acceptedBy(),
            invitation.acceptedAt()
        ));
        return toDomain(saved);
    }

    @Override
    public Optional<Invitation> findById(String invitationId) {
        return repository.findById(invitationId).map(this::toDomain);
    }

    private Invitation toDomain(InvitationJpaEntity entity) {
        return Invitation.reconstitute(
            entity.getInvitationId(),
            entity.getTenantId(),
            entity.getGroupId(),
            Email.of(entity.getInviteeEmail()),
            entity.getRole(),
            entity.getStatus(),
            entity.getInvitedBy(),
            entity.getInvitedAt(),
            entity.getAcceptedBy(),
            entity.getAcceptedAt()
        );
    }
}
