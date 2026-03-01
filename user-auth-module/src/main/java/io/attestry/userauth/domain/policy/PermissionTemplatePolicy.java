package io.attestry.userauth.domain.policy;

import io.attestry.userauth.domain.auth.model.PermissionCodes;
import java.util.List;
import java.util.Locale;

public final class PermissionTemplatePolicy {

    private PermissionTemplatePolicy() {
    }

    public static List<String> resolvePermissionCodes(String templateCode) {
        String normalized = normalize(templateCode);
        return switch (normalized) {
            case "TEMPLATE_BRAND_WORK" -> List.of(
                PermissionCodes.BRAND_MINT,
                PermissionCodes.BRAND_VOID
            );
            case "TEMPLATE_RETAIL_WORK" -> List.of(
                PermissionCodes.RETAIL_RELEASE,
                PermissionCodes.RETAIL_TRANSFER_CREATE
            );
            default -> throw new IllegalArgumentException("Unknown permission template: " + templateCode);
        };
    }

    public static String normalize(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            throw new IllegalArgumentException("templateCode is required");
        }
        return templateCode.trim().toUpperCase(Locale.ROOT);
    }
}
