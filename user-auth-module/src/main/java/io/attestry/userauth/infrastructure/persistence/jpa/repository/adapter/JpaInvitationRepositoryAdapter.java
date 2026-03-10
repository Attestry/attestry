package io.attestry.userauth.infrastructure.persistence.jpa.repository.adapter;

import io.attestry.userauth.application.port.InvitationPort;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.infrastructure.persistence.jpa.mapper.InvitationMapper;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.InvitationJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInvitationRepositoryAdapter implements InvitationPort {

    private final InvitationJpaRepository repository;
    private final InvitationMapper mapper;

    public JpaInvitationRepositoryAdapter(InvitationJpaRepository repository, InvitationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Invitation save(Invitation invitation) {
        return mapper.toDomain(repository.save(mapper.toEntity(invitation)));
    }

    @Override
    public Optional<Invitation> findById(String invitationId) {
        return repository.findById(invitationId).map(mapper::toDomain);
    }
}
