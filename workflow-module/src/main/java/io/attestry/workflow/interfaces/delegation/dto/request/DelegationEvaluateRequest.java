package io.attestry.workflow.interfaces.delegation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DelegationEvaluateRequest(
    String sourceTenantId,
    String targetTenantId,
    @NotBlank(message = "Brand tenant ID is required")
    String brandTenantId,
    @NotBlank(message = "Partner tenant ID is required")
    String partnerTenantId,
    @NotBlank(message = "Resource type is required")
    String resourceType,
    @NotBlank(message = "Resource ID is required")
    String resourceId,
    @NotBlank(message = "Permission code is required")
    String permissionCode
) {
    public String resolvedSourceTenantId() {
        if (sourceTenantId != null && !sourceTenantId.isBlank()) {
            return sourceTenantId;
        }
        return brandTenantId;
    }

    public String resolvedTargetTenantId() {
        if (targetTenantId != null && !targetTenantId.isBlank()) {
            return targetTenantId;
        }
        return partnerTenantId;
    }
}
