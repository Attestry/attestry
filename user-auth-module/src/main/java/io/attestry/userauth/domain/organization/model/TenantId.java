package io.attestry.userauth.domain.organization.model;

import java.util.UUID;

public record TenantId(String value) {
    public static TenantId of(String value) { return new TenantId(value); }
    public static TenantId generate() { return new TenantId(UUID.randomUUID().toString()); }
    @Override public String toString() { return value; }
}
