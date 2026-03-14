package io.attestry.product.domain.permission.model;

import java.time.Instant;

public class PassportPermission {

    private final String permissionId;
    private final String passportId;
    private final String sellerTenantId;
    private final PermissionScope scope;
    private PermissionStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;

    private PassportPermission(
        String permissionId,
        String passportId,
        String sellerTenantId,
        PermissionScope scope,
        PermissionStatus status,
        Instant expiresAt,
        Instant createdAt
    ) {
        this.permissionId = permissionId;
        this.passportId = passportId;
        this.sellerTenantId = sellerTenantId;
        this.scope = scope;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static PassportPermission grant(
        String permissionId,
        String passportId,
        String sellerTenantId,
        PermissionScope scope,
        Instant expiresAt,
        Instant now
    ) {
        return new PassportPermission(
            permissionId, passportId, sellerTenantId,
            scope, PermissionStatus.ACTIVE, expiresAt, now
        );
    }

    public static PassportPermission reconstitute(
        String permissionId,
        String passportId,
        String sellerTenantId,
        PermissionScope scope,
        PermissionStatus status,
        Instant expiresAt,
        Instant createdAt
    ) {
        return new PassportPermission(
            permissionId, passportId, sellerTenantId,
            scope, status, expiresAt, createdAt
        );
    }

    public void revoke() { this.status = PermissionStatus.REVOKED; }

    public void suspend() { this.status = PermissionStatus.SUSPENDED; }

    public boolean isActive(Instant now) {
        return status == PermissionStatus.ACTIVE
            && (expiresAt == null || expiresAt.isAfter(now));
    }

    public String getPermissionId() { return permissionId; }
    public String getPassportId() { return passportId; }
    public String getSellerTenantId() { return sellerTenantId; }
    public PermissionScope getScope() { return scope; }
    public PermissionStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
