package io.attestry.userauth.domain.authorization.service;

import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.authorization.policy.TemplateApplicationPolicy;
import java.util.Set;

public class TemplateApplicationDomainService {

    private final TemplateApplicationPolicy templateApplicationPolicy;

    public TemplateApplicationDomainService(TemplateApplicationPolicy templateApplicationPolicy) {
        this.templateApplicationPolicy = templateApplicationPolicy;
    }

    public String assertCanMutateTemplate(Set<String> actorRoleCodes, String requestedTemplateCode, boolean templateEnabled) {
        Evaluation evaluation = evaluate(actorRoleCodes, requestedTemplateCode, templateEnabled);
        switch (evaluation.denialReason()) {
            case INVALID_TEMPLATE_CODE -> throw new UserAuthDomainException(
                    UserAuthErrorCode.INVALID_REQUEST, "Invalid template code");
            case ACTOR_NOT_ALLOWED -> throw new UserAuthDomainException(
                    UserAuthErrorCode.FORBIDDEN_SCOPE, "Permission template operation requires TENANT_OWNER");
            case TEMPLATE_DISABLED -> throw new UserAuthDomainException(
                    UserAuthErrorCode.FORBIDDEN_SCOPE, "Permission template is disabled");
            case NONE -> { /* allowed */ }
        }
        return evaluation.normalizedTemplateCode();
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
