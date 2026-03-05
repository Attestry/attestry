package io.attestry.userauth.application.policy;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.authorization.policy.PermissionCatalog;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class StaticPermissionCatalog implements PermissionCatalog {

    private static final Map<String, PermissionDefinition> DEFINITIONS = buildDefinitions();

    @Override
    public Optional<PermissionDefinition> findByCode(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(DEFINITIONS.get(permissionCode.trim().toUpperCase(Locale.ROOT)));
    }

    @Override
    public Set<PermissionDefinition> all() {
        return Set.copyOf(DEFINITIONS.values());
    }

    private static Map<String, PermissionDefinition> buildDefinitions() {
        Map<String, PermissionDefinition> map = new LinkedHashMap<>();
        add(map, PermissionCodes.PLATFORM_ADMIN, "Platform Admin", "Platform-wide administration", "platform", "admin");
        add(map, PermissionCodes.TENANT_CREATE_APPROVE, "Tenant Create Approve", "Approve tenant onboarding", "tenant", "approve");
        add(map, PermissionCodes.TENANT_SUSPEND, "Tenant Suspend", "Suspend tenant", "tenant", "suspend");
        add(map, PermissionCodes.GLOBAL_AUDIT_READ, "Global Audit Read", "Read global audit logs", "audit", "read");
        add(map, PermissionCodes.TENANT_GROUP_SUSPEND, "Tenant Group Suspend", "Suspend tenant group", "group", "suspend");
        add(map, PermissionCodes.TENANT_GROUP_RESUME, "Tenant Group Resume", "Resume tenant group", "group", "resume");
        add(map, PermissionCodes.TENANT_INVITATION_CREATE, "Tenant Invitation Create", "Create tenant invitation", "invitation", "create");
        add(map, PermissionCodes.TENANT_INVITATION_REVOKE, "Tenant Invitation Revoke", "Revoke tenant invitation", "invitation", "revoke");
        add(map, PermissionCodes.TENANT_INVITATION_VIEW, "Tenant Invitation View", "View tenant invitation", "invitation", "view");
        add(map, PermissionCodes.TENANT_MEMBERSHIP_VIEW, "Tenant Membership View", "View memberships", "membership", "view");
        add(map, PermissionCodes.TENANT_READ_ONLY, "Tenant Read Only", "Read-only access for tenant scoped resources", "tenant", "read");
        add(map, PermissionCodes.TENANT_ROLE_ASSIGN, "Tenant Role Assign", "Assign/revoke membership roles", "membership", "assign");
        add(map, PermissionCodes.TENANT_MEMBERSHIP_ENFORCE, "Tenant Membership Enforce", "Enforce membership status", "membership", "enforce");
        add(map, PermissionCodes.PARTNER_LINK_CREATE, "Partner Link Create", "Create partner links", "partner_link", "create");
        add(map, PermissionCodes.PARTNER_LINK_READ, "Partner Link Read", "Read partner links", "partner_link", "read");
        add(map, PermissionCodes.PARTNER_LINK_SUSPEND, "Partner Link Suspend", "Suspend partner links", "partner_link", "suspend");
        add(map, PermissionCodes.PARTNER_LINK_RESUME, "Partner Link Resume", "Resume partner links", "partner_link", "resume");
        add(map, PermissionCodes.PARTNER_LINK_TERMINATE, "Partner Link Terminate", "Terminate partner links", "partner_link", "terminate");
        add(map, PermissionCodes.PARTNER_LINK_APPROVE, "Partner Link Approve", "Approve/reject partner links", "partner_link", "approve");
        add(map, PermissionCodes.DELEGATION_GRANT, "Delegation Grant", "Grant delegation", "delegation", "grant");
        add(map, PermissionCodes.DELEGATION_REVOKE, "Delegation Revoke", "Revoke delegation", "delegation", "revoke");
        add(map, PermissionCodes.DELEGATION_READ, "Delegation Read", "Read delegation", "delegation", "read");
        add(map, PermissionCodes.BRAND_RELEASE, "Brand Release", "Release brand assets", "brand", "release");
        add(map, PermissionCodes.BRAND_MINT, "Brand Mint", "Mint brand assets", "brand", "mint");
        add(map, PermissionCodes.BRAND_VOID, "Brand Void", "Void brand assets", "brand", "void");
        add(map, PermissionCodes.RETAIL_TRANSFER_CREATE, "Retail Transfer Create", "Create retail transfer", "retail", "transfer_create");
        add(map, PermissionCodes.PASSPORT_PERMISSION_GRANT, "Passport Permission Grant", "Grant passport permissions", "passport", "grant");
        add(map, PermissionCodes.TENANT_AUDIT_READ, "Tenant Audit Read", "Read tenant audit logs", "audit", "read");
        add(map, PermissionCodes.OWNER_TRANSFER_CREATE, "Owner Transfer Create", "Create owner transfer", "owner", "transfer_create");
        add(map, PermissionCodes.OWNER_TRANSFER_ACCEPT, "Owner Transfer Accept", "Accept owner transfer", "owner", "transfer_accept");
        add(map, PermissionCodes.OWNER_RISK_FLAG, "Owner Risk Flag", "Flag owner risk", "owner", "risk_flag");
        add(map, PermissionCodes.OWNER_RISK_CLEAR, "Owner Risk Clear", "Clear owner risk", "owner", "risk_clear");
        add(map, PermissionCodes.PURCHASE_CLAIM_APPROVE, "Purchase Claim Approve", "Approve purchase claims", "claim", "approve");
        add(map, PermissionCodes.OWNER_SERVICE_CREATE, "Owner Service Create", "Create service request as owner", "owner", "service_create");
        add(map, PermissionCodes.SERVICE_COMPLETE, "Service Complete", "Complete service request as provider", "service", "complete");
        return Map.copyOf(map);
    }

    private static void add(
        Map<String, PermissionDefinition> map,
        String code,
        String name,
        String description,
        String resourceType,
        String action
    ) {
        map.put(code, new PermissionDefinition(
            permissionId(code),
            code,
            name,
            description,
            resourceType,
            action,
            true
        ));
    }

    private static String permissionId(String code) {
        String normalized = code.toLowerCase(Locale.ROOT).replace('_', '-');
        String id = "perm-" + normalized;
        return id.length() <= 36 ? id : id.substring(0, 36);
    }
}
