package io.attestry.userauth.application.port.membership;

import io.attestry.userauth.domain.membership.model.Invitation;

import java.util.Optional;

public interface InvitationPort {
    Invitation save(Invitation invitation);

    Optional<Invitation> findById(String invitationId);
}
