package io.attestry.userauth.domain.membership.repository;

import io.attestry.userauth.domain.membership.model.Invitation;
import java.util.Optional;

public interface InvitationRepository {
    Invitation save(Invitation invitation);

    Optional<Invitation> findById(String invitationId);
}
