package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class RoleJpaEntity {

    @Id
    @Column(name = "role_id", nullable = false, length = 36)
    private String roleId;

    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected RoleJpaEntity() {
    }

    public String getRoleId() {
        return roleId;
    }

    public String getCode() {
        return code;
    }
}
