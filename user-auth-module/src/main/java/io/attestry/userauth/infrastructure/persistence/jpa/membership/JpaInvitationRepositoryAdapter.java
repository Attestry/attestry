package io.attestry.userauth.infrastructure.persistence.jpa.membership;

import io.attestry.userauth.application.port.membership.InvitationPort;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.infrastructure.persistence.jpa.mapper.InvitationMapper;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.InvitationJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaInvitationRepositoryAdapter implements InvitationPort {

    private final InvitationJpaRepository repository;
    private final InvitationMapper mapper;

    @Override
    public Invitation save(Invitation invitation) {
        return mapper.toDomain(repository.save(mapper.toEntity(invitation)));
    }

    @Override
    public Optional<Invitation> findById(String invitationId) {
        return repository.findById(invitationId).map(mapper::toDomain);
    }
}
