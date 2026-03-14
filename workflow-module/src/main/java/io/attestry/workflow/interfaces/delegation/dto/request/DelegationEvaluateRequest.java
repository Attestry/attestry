package io.attestry.workflow.interfaces.delegation.dto.request;

public record DelegationEvaluateRequest(
    String sourceTenantId,
    String targetTenantId,
    String brandTenantId,
    String partnerTenantId,
    String resourceType,
    String resourceId,
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
