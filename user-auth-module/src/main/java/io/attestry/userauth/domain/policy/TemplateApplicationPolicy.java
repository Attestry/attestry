package io.attestry.userauth.domain.policy;

import java.util.Set;

public interface TemplateApplicationPolicy {

    boolean canMutateTemplate(Set<String> actorRoleCodes);

    String normalizeTemplateCode(String templateCode);
}
