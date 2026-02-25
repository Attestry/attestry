package io.attestry.userauth.domain.organization.model;

public record Group(
    String groupId,
    String tenantId,
    GroupType type,
    GroupStatus status
) {
    public boolean isActive() {
        return status == GroupStatus.ACTIVE;
    }
}
