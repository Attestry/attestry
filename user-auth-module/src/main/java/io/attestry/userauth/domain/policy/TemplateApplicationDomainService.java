package io.attestry.userauth.domain.policy;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TemplateApplicationDomainService {

    private final TemplateApplicationPolicy templateApplicationPolicy;

    public TemplateApplicationDomainService(TemplateApplicationPolicy templateApplicationPolicy) {
        this.templateApplicationPolicy = templateApplicationPolicy;
    }

    public Evaluation evaluate(Set<String> actorRoleCodes, String requestedTemplateCode, boolean templateEnabled) {
        String normalizedTemplateCode = templateApplicationPolicy.normalizeTemplateCode(requestedTemplateCode);
        if (normalizedTemplateCode == null) {
            return new Evaluation(null, DenialReason.INVALID_TEMPLATE_CODE);
        }
        if (!templateApplicationPolicy.canMutateTemplate(actorRoleCodes)) {
            return new Evaluation(normalizedTemplateCode, DenialReason.ACTOR_NOT_ALLOWED);
        }
        if (!templateEnabled) {
            return new Evaluation(normalizedTemplateCode, DenialReason.TEMPLATE_DISABLED);
        }
        return new Evaluation(normalizedTemplateCode, DenialReason.NONE);
    }

    public enum DenialReason {
        NONE,
        INVALID_TEMPLATE_CODE,
        ACTOR_NOT_ALLOWED,
        TEMPLATE_DISABLED
    }

    public record Evaluation(
        String normalizedTemplateCode,
        DenialReason denialReason
    ) {
        public boolean denied() {
            return denialReason != DenialReason.NONE;
        }
    }
}
