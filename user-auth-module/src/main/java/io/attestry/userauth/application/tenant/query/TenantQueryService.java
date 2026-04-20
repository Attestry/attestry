package io.attestry.userauth.application.tenant.query;

import io.attestry.userauth.application.port.tenant.TenantRepositoryPort;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.tenant.model.Tenant;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import io.attestry.userauth.domain.tenant.model.TenantType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantQueryService implements TenantQueryUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public TenantView getTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.TENANT_NOT_FOUND, "Tenant not found"));
        return toResult(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, TenantView> getTenants(List<String> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return Map.of();
        }
        List<String> distinctTenantIds = tenantIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (distinctTenantIds.isEmpty()) {
            return Map.of();
        }
        return tenantRepositoryPort.findByIds(distinctTenantIds).stream()
            .collect(LinkedHashMap::new,
                (map, tenant) -> map.put(tenant.tenantId(), toResult(tenant)),
                LinkedHashMap::putAll);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantPageView listTenants(String type, String status, String name, int page, int size) {
        validatePage(page, size);
        String trimmedName = (name == null || name.isBlank()) ? null : name.trim();
        Page<Tenant> tenantPage = tenantRepositoryPort.findPage(
                parseTypeOrNull(type),
                parseStatusOrNull(status),
                trimmedName,
                PageRequest.of(page, size));
        return new TenantPageView(
                tenantPage.getContent().stream().map(this::toResult).toList(),
                tenantPage.getNumber(),
                tenantPage.getSize(),
                tenantPage.getTotalElements(),
                tenantPage.getTotalPages());
    }

    private TenantType parseTypeOrNull(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return TenantType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST,
                    "type must be BRAND, RETAIL, SERVICE, or INTERNAL");
        }
    }

    private TenantStatus parseStatusOrNull(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TenantStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST,
                    "status must be ACTIVE or SUSPENDED");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST, "page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST, "size must be between 1 and 100");
        }
    }

    private TenantView toResult(Tenant tenant) {
        return new TenantView(
                tenant.tenantId(),
                tenant.name(),
                tenant.region(),
                tenant.address(),
                tenant.type().name(),
                tenant.status().name());
    }
}
