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

    public Group suspend() {
        if (status == GroupStatus.SUSPENDED) {
            return this;
        }
        return new Group(groupId, tenantId, type, GroupStatus.SUSPENDED);
    }

    public Group unsuspend() {
        if (status == GroupStatus.ACTIVE) {
            return this;
        }
        return new Group(groupId, tenantId, type, GroupStatus.ACTIVE);
    }
}
