package io.attestry.userauth.domain.onboarding.policy;

import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.authorization.policy.SystemPermissionTemplateCatalog;
import io.attestry.userauth.domain.tenant.model.TenantType;
import java.util.ArrayList;
import java.util.List;

public class OnboardingTemplateBindingPolicy {

    public List<TemplateBindingRule> resolveDefaultBindings(TenantType type) {
        List<TemplateBindingRule> bindings = new ArrayList<>();

        bindings.add(new TemplateBindingRule(RoleCodes.TENANT_OWNER, SystemPermissionTemplateCatalog.TEMPLATE_TENANT_OWNER_CORE));
        bindings.add(new TemplateBindingRule(RoleCodes.TENANT_OWNER, SystemPermissionTemplateCatalog.TEMPLATE_TENANT_READ_ONLY));
        bindings.add(new TemplateBindingRule(RoleCodes.TENANT_OPERATOR, SystemPermissionTemplateCatalog.TEMPLATE_TENANT_READ_ONLY));

        String operatorWorkTemplateCode = switch (type) {
            case BRAND -> SystemPermissionTemplateCatalog.TEMPLATE_BRAND_WORK;
            case RETAIL -> SystemPermissionTemplateCatalog.TEMPLATE_RETAIL_WORK;
            case SERVICE -> SystemPermissionTemplateCatalog.TEMPLATE_SERVICE_WORK;
            default -> null;
        };
        if (operatorWorkTemplateCode != null) {
            bindings.add(new TemplateBindingRule(RoleCodes.TENANT_OPERATOR, operatorWorkTemplateCode));
        }

        bindings.add(new TemplateBindingRule(RoleCodes.TENANT_STAFF, SystemPermissionTemplateCatalog.TEMPLATE_TENANT_READ_ONLY));
        return List.copyOf(bindings);
    }

    public record TemplateBindingRule(String roleCode, String templateCode) {
    }
}
