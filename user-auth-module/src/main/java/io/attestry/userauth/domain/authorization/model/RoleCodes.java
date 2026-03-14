package io.attestry.userauth.domain.authorization.model;

public final class RoleCodes {

    private RoleCodes() {
    }

    public static final String PLATFORM_SUPER_ADMIN = "PLATFORM_SUPER_ADMIN";
    public static final String OWNER_DEFAULT = "OWNER_DEFAULT";
    public static final String TENANT_OWNER = "TENANT_OWNER";
    public static final String TENANT_OPERATOR = "TENANT_OPERATOR";
    public static final String TENANT_STAFF = "TENANT_STAFF";
}
