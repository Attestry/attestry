package io.attestry.userauth.domain.authorization.policy;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import java.util.List;
import java.util.Map;

public final class SystemPermissionTemplateCatalog {

    private SystemPermissionTemplateCatalog() {
    }

    public static final String TEMPLATE_TENANT_OWNER_CORE = "TEMPLATE_TENANT_OWNER_CORE";
    public static final String TEMPLATE_BRAND_WORK = "TEMPLATE_BRAND_WORK";
    public static final String TEMPLATE_RETAIL_WORK = "TEMPLATE_RETAIL_WORK";

    public static Map<String, TemplateDefinition> defaults() {
        return Map.of(
            TEMPLATE_TENANT_OWNER_CORE,
            new TemplateDefinition(
                "tpl-tenant-owner-core",
                TEMPLATE_TENANT_OWNER_CORE,
                "Tenant Owner Core Template",
                "Core tenant owner permissions",
                List.of(
                    PermissionCodes.TENANT_GROUP_SUSPEND,
                    PermissionCodes.TENANT_GROUP_RESUME,
                    PermissionCodes.TENANT_INVITATION_CREATE,
                    PermissionCodes.TENANT_INVITATION_REVOKE,
                    PermissionCodes.TENANT_INVITATION_VIEW,
                    PermissionCodes.TENANT_MEMBERSHIP_VIEW,
                    PermissionCodes.TENANT_ROLE_ASSIGN,
                    PermissionCodes.TENANT_MEMBERSHIP_ENFORCE,
                    PermissionCodes.TENANT_AUDIT_READ,
                    PermissionCodes.PARTNER_LINK_CREATE,
                    PermissionCodes.PARTNER_LINK_READ,
                    PermissionCodes.PARTNER_LINK_SUSPEND,
                    PermissionCodes.PARTNER_LINK_RESUME,
                    PermissionCodes.PARTNER_LINK_TERMINATE,
                    PermissionCodes.PARTNER_LINK_APPROVE,
                    PermissionCodes.DELEGATION_GRANT,
                    PermissionCodes.DELEGATION_REVOKE,
                    PermissionCodes.DELEGATION_READ
                )
            ),
            TEMPLATE_BRAND_WORK,
            new TemplateDefinition(
                "tpl-brand-work",
                TEMPLATE_BRAND_WORK,
                "Brand Work Template",
                "Brand operator work permissions",
                List.of(PermissionCodes.BRAND_MINT, PermissionCodes.BRAND_VOID, PermissionCodes.BRAND_RELEASE)
            ),
            TEMPLATE_RETAIL_WORK,
            new TemplateDefinition(
                "tpl-retail-work",
                TEMPLATE_RETAIL_WORK,
                "Retail Work Template",
                "Retail operator work permissions",
                List.of(PermissionCodes.RETAIL_TRANSFER_CREATE)
            )
        );
    }

    public record TemplateDefinition(
        String templateId,
        String code,
        String name,
        String description,
        List<String> permissionCodes
    ) {
    }
}
