package io.attestry.userauth.domain.organization.event;

import io.attestry.userauth.domain.organization.model.GroupStatus;

public record GroupStatusChangedEvent(
    String tenantId,
    String groupId,
    GroupStatus newStatus
) {
}
