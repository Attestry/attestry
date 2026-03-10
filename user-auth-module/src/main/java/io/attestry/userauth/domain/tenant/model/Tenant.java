package io.attestry.userauth.domain.tenant.model;

import io.attestry.commonlib.domain.AggregateRoot;

import java.util.UUID;

public class Tenant extends AggregateRoot {

    private final String tenantId;
    private final String name;
    private final String region;
    private final String address;
    private final TenantType type;
    private TenantStatus status;

    private Tenant(String tenantId, String name, String region, String address, TenantType type, TenantStatus status) {
        this.tenantId = tenantId;
        this.name = name;
        this.region = region;
        this.address = address;
        this.type = type;
        this.status = status;
    }

    public static Tenant create(String name, String region, TenantType type) {
        return new Tenant(UUID.randomUUID().toString(), name, region, null, type, TenantStatus.ACTIVE);
    }

    public static Tenant create(String name, String region, String address, TenantType type) {
        return new Tenant(UUID.randomUUID().toString(), name, region, address, type, TenantStatus.ACTIVE);
    }

    public static Tenant reconstitute(String tenantId, String name, String region, String address,
                                       TenantType type, TenantStatus status) {
        return new Tenant(tenantId, name, region, address, type, status);
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
    public String address() { return address; }
    public TenantType type() { return type; }
    public TenantStatus status() { return status; }
}
