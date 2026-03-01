package io.attestry.userauth.domain.auth.model;

public final class RoleCodes {

    private RoleCodes() {
    }

    public static final String PLATFORM_SUPER_ADMIN = "PLATFORM_SUPER_ADMIN";
    public static final String OWNER_DEFAULT = "OWNER_DEFAULT";
    public static final String TENANT_OWNER = "TENANT_OWNER";
    public static final String TENANT_MEMBERSHIP_ADMIN = "TENANT_MEMBERSHIP_ADMIN";
    public static final String TENANT_PASSPORT_ADMIN = "TENANT_PASSPORT_ADMIN";
    public static final String BRAND_ADMIN_BASE = "BRAND_ADMIN_BASE";
    public static final String RETAIL_ADMIN_BASE = "RETAIL_ADMIN_BASE";
    public static final String BRAND_OPERATOR = "BRAND_OPERATOR";
    public static final String RETAIL_OPERATOR = "RETAIL_OPERATOR";
    public static final String GROUP_STAFF = "GROUP_STAFF";
}
