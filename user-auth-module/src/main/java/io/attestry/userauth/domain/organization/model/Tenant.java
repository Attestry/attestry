package io.attestry.userauth.domain.organization.model;

import java.util.UUID;

public class Tenant {

    private final String tenantId;
    private final String name;
    private final String region;
    private final TenantType type;
    private TenantStatus status;

    private Tenant(String tenantId, String name, String region, TenantType type, TenantStatus status) {
        this.tenantId = tenantId;
        this.name = name;
        this.region = region;
        this.type = type;
        this.status = status;
    }

    public static Tenant create(String name, String region, TenantType type) {
        return new Tenant(UUID.randomUUID().toString(), name, region, type, TenantStatus.ACTIVE);
    }

    public static Tenant reconstitute(String tenantId, String name, String region,
                                       TenantType type, TenantStatus status) {
        return new Tenant(tenantId, name, region, type, status);
    }

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
    }

    public void unsuspend() {
        this.status = TenantStatus.ACTIVE;
    }

    // Getters
    public String tenantId() { return tenantId; }
    public String name() { return name; }
    public String region() { return region; }
    public TenantType type() { return type; }
    public TenantStatus status() { return status; }
}
