package io.attestry.userauth.domain.organization.model;

import io.attestry.userauth.domain.organization.event.GroupStatusChangedEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Tenant {

    private final String tenantId;
    private final String name;
    private final String region;
    private TenantStatus status;
    private final List<Group> groups;
    private final List<Object> domainEvents = new ArrayList<>();

    private Tenant(String tenantId, String name, String region, TenantStatus status, List<Group> groups) {
        this.tenantId = tenantId;
        this.name = name;
        this.region = region;
        this.status = status;
        this.groups = new ArrayList<>(groups);
    }

    public static Tenant create(String name, String region) {
        return new Tenant(UUID.randomUUID().toString(), name, region, TenantStatus.ACTIVE, List.of());
    }

    public static Tenant reconstitute(String tenantId, String name, String region,
                                       TenantStatus status, List<Group> groups) {
        return new Tenant(tenantId, name, region, status, groups);
    }

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    public Group addGroup(GroupType type) {
        Group group = Group.create(tenantId, type);
        groups.add(group);
        return group;
    }

    public void suspendGroup(String groupId) {
        Group group = findGroup(groupId);
        if (group.suspend()) {
            domainEvents.add(new GroupStatusChangedEvent(tenantId, groupId, GroupStatus.SUSPENDED));
        }
    }

    public void unsuspendGroup(String groupId) {
        Group group = findGroup(groupId);
        if (group.unsuspend()) {
            domainEvents.add(new GroupStatusChangedEvent(tenantId, groupId, GroupStatus.ACTIVE));
        }
    }

    private Group findGroup(String groupId) {
        return groups.stream()
            .filter(g -> g.groupId().equals(groupId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Group not found in tenant: " + groupId));
    }

    public List<Object> harvestEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    // Getters
    public String tenantId() { return tenantId; }
    public String name() { return name; }
    public String region() { return region; }
    public TenantStatus status() { return status; }
    public List<Group> groups() { return Collections.unmodifiableList(groups); }
}
