package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.organization.model.Group;

public interface GroupRepositoryPort {
    Group save(Group group);
}
