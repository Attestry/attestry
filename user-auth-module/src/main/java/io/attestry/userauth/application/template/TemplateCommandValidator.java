package io.attestry.userauth.application.template;

import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemplateCommandValidator {

    public String normalizeTemplateCode(String code) {
        return normalizeRequired(code, "templateCode").toUpperCase(Locale.ROOT);
    }

    public String normalizeRoleCode(String code) {
        return normalizeRequired(code, "roleCode").toUpperCase(Locale.ROOT);
    }

    public Set<String> normalizePermissionCodes(List<String> permissionCodes) {
        if (permissionCodes == null) {
            return Set.of();
        }
        return permissionCodes.stream()
            .map(this::normalizePermissionCode)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public String normalizePermissionCode(String code) {
        return normalizeRequired(code, "permissionCode").toUpperCase(Locale.ROOT);
    }

    public String normalizeResourceType(String resourceType) {
        return normalizeRequired(resourceType, "resourceType").toUpperCase(Locale.ROOT);
    }

    public String normalizeAction(String action) {
        return normalizeRequired(action, "action").toUpperCase(Locale.ROOT);
    }

    public String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }

    public String normalizeOptional(String value) {
        return value == null ? null : value.trim();
    }

    public void validateUpdateHasAtLeastOneField(Object name, Object description, Object enabled) {
        if (name == null && description == null && enabled == null) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST, "At least one field must be provided");
        }
    }
}
