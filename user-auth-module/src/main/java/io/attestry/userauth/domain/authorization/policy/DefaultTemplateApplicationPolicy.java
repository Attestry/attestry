package io.attestry.userauth.domain.authorization.policy;

import io.attestry.userauth.domain.authorization.model.RoleCodes;
import java.util.Locale;
import java.util.Set;

public class DefaultTemplateApplicationPolicy implements TemplateApplicationPolicy {

    @Override
    public boolean canMutateTemplate(Set<String> actorRoleCodes) {
        if (actorRoleCodes == null) {
            return false;
        }
        return actorRoleCodes.contains(RoleCodes.TENANT_OWNER) || actorRoleCodes.contains(RoleCodes.PLATFORM_SUPER_ADMIN);
    }

    @Override
    public String normalizeTemplateCode(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            return null;
        }
        return templateCode.trim().toUpperCase(Locale.ROOT);
    }
}
