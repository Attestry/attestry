package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_groups")
public class GroupJpaEntity {

    @Id
    @Column(name = "group_id", nullable = false, length = 36)
    private String groupId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private GroupType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GroupStatus status;

    protected GroupJpaEntity() {
    }

    public GroupJpaEntity(String groupId, String tenantId, GroupType type, GroupStatus status) {
        this.groupId = groupId;
        this.tenantId = tenantId;
        this.type = type;
        this.status = status;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public GroupType getType() {
        return type;
    }

    public GroupStatus getStatus() {
        return status;
    }
}
