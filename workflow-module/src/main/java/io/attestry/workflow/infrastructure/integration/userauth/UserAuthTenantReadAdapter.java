package io.attestry.workflow.infrastructure.integration.userauth;

import io.attestry.userauth.application.tenant.usecase.TenantQueryUseCase;
import io.attestry.userauth.application.tenant.view.TenantView;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.workflow.application.port.common.TenantReadPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserAuthTenantReadAdapter implements TenantReadPort {

    private static final String ACTIVE_TENANT_STATUS = "ACTIVE";

    private final TenantQueryUseCase tenantQueryUseCase;

    @Override
    public boolean existsActiveTenant(String tenantId) {
        try {
            return ACTIVE_TENANT_STATUS.equals(tenantQueryUseCase.getTenant(tenantId).status());
        } catch (UserAuthDomainException ex) {
            if (ex.getErrorCode() == UserAuthErrorCode.TENANT_NOT_FOUND) {
                return false;
            }
            throw ex;
        }
    }

    @Override
    public TenantSummary findTenantSummary(String tenantId) {
        return findTenantSummariesByIds(List.of(tenantId)).get(tenantId);
    }

    @Override
    public Map<String, TenantSummary> findTenantSummariesByIds(List<String> tenantIds) {
        return tenantQueryUseCase.getTenants(sanitizeIds(tenantIds)).values().stream()
            .collect(LinkedHashMap::new,
                (map, tenant) -> map.put(tenant.tenantId(), toSummary(tenant)),
                LinkedHashMap::putAll);
    }

    private List<String> sanitizeIds(List<String> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return List.of();
        }
        return tenantIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private TenantSummary toSummary(TenantView tenant) {
        return new TenantSummary(
            tenant.tenantId(),
            tenant.name(),
            tenant.region(),
            tenant.address(),
            tenant.type()
        );
    }
}
