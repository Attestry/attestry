package io.attestry.userauth.domain.authorization.model;

public final class PermissionCodes {

    private PermissionCodes() {
    }

    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    public static final String TENANT_CREATE_APPROVE = "TENANT_CREATE_APPROVE";
    public static final String TENANT_SUSPEND = "TENANT_SUSPEND";
    public static final String GLOBAL_AUDIT_READ = "GLOBAL_AUDIT_READ";
    public static final String TENANT_GROUP_SUSPEND = "TENANT_GROUP_SUSPEND";
    public static final String TENANT_GROUP_RESUME = "TENANT_GROUP_RESUME";
    public static final String TENANT_INVITATION_CREATE = "TENANT_INVITATION_CREATE";
    public static final String TENANT_INVITATION_REVOKE = "TENANT_INVITATION_REVOKE";
    public static final String TENANT_INVITATION_VIEW = "TENANT_INVITATION_VIEW";
    public static final String TENANT_MEMBERSHIP_VIEW = "TENANT_MEMBERSHIP_VIEW";
    public static final String TENANT_READ_ONLY = "TENANT_READ_ONLY";
    public static final String TENANT_ROLE_ASSIGN = "TENANT_ROLE_ASSIGN";
    public static final String TENANT_MEMBERSHIP_ENFORCE = "TENANT_MEMBERSHIP_ENFORCE";
    public static final String PARTNER_LINK_CREATE = "PARTNER_LINK_CREATE";
    public static final String PARTNER_LINK_READ = "PARTNER_LINK_READ";
    public static final String PARTNER_LINK_SUSPEND = "PARTNER_LINK_SUSPEND";
    public static final String PARTNER_LINK_RESUME = "PARTNER_LINK_RESUME";
    public static final String PARTNER_LINK_TERMINATE = "PARTNER_LINK_TERMINATE";
    public static final String PARTNER_LINK_APPROVE = "PARTNER_LINK_APPROVE";
    public static final String DELEGATION_GRANT = "DELEGATION_GRANT";
    public static final String DELEGATION_REVOKE = "DELEGATION_REVOKE";
    public static final String DELEGATION_READ = "DELEGATION_READ";
    public static final String BRAND_RELEASE = "BRAND_RELEASE";
    public static final String BRAND_MINT = "BRAND_MINT";
    public static final String BRAND_VOID = "BRAND_VOID";
    public static final String RETAIL_TRANSFER_CREATE = "RETAIL_TRANSFER_CREATE";
    public static final String PASSPORT_PERMISSION_GRANT = "PASSPORT_PERMISSION_GRANT";
    public static final String TENANT_AUDIT_READ = "TENANT_AUDIT_READ";
    public static final String OWNER_TRANSFER_CREATE = "OWNER_TRANSFER_CREATE";
    public static final String OWNER_TRANSFER_ACCEPT = "OWNER_TRANSFER_ACCEPT";
    public static final String OWNER_RISK_FLAG = "OWNER_RISK_FLAG";
    public static final String OWNER_RISK_CLEAR = "OWNER_RISK_CLEAR";
    public static final String PURCHASE_CLAIM_APPROVE = "PURCHASE_CLAIM_APPROVE";
    public static final String OWNER_SERVICE_CREATE = "OWNER_SERVICE_CREATE";
    public static final String SERVICE_COMPLETE = "SERVICE_COMPLETE";
}
