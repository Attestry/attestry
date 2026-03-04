package io.attestry.userauth.domain.organization.model;

import java.util.UUID;

public class Group {

    private final String groupId;
    private final String tenantId;
    private final GroupType type;
    private GroupStatus status;

    private Group(String groupId, String tenantId, GroupType type, GroupStatus status) {
        this.groupId = groupId;
        this.tenantId = tenantId;
        this.type = type;
        this.status = status;
    }

    public static Group create(String tenantId, GroupType type) {
        return new Group(UUID.randomUUID().toString(), tenantId, type, GroupStatus.ACTIVE);
    }

    public static Group reconstitute(String groupId, String tenantId, GroupType type, GroupStatus status) {
        return new Group(groupId, tenantId, type, status);
    }

    public boolean isActive() {
        return status == GroupStatus.ACTIVE;
    }

    public boolean suspend() {
        if (status == GroupStatus.SUSPENDED) {
            return false;
        }
        this.status = GroupStatus.SUSPENDED;
        return true;
    }

    public boolean unsuspend() {
        if (status == GroupStatus.ACTIVE) {
            return false;
        }
        this.status = GroupStatus.ACTIVE;
        return true;
    }

    // Getters
    public String groupId() { return groupId; }
    public String tenantId() { return tenantId; }
    public GroupType type() { return type; }
    public GroupStatus status() { return status; }
}
