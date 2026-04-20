package io.attestry.userauth.application.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.tenant.query.TenantQueryService;
import io.attestry.userauth.application.tenant.query.TenantView;
import io.attestry.userauth.application.port.tenant.TenantRepositoryPort;
import io.attestry.userauth.domain.tenant.model.Tenant;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import io.attestry.userauth.domain.tenant.model.TenantType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class TenantQueryServiceTest {

    @Test
    void getTenants_returnsDistinctTenantMap() {
        Tenant brand = Tenant.reconstitute("tenant-1", "Brand One", "KR", "addr1", TenantType.BRAND, TenantStatus.ACTIVE);
        Tenant retail = Tenant.reconstitute("tenant-2", "Retail Two", "US", "addr2", TenantType.RETAIL, TenantStatus.SUSPENDED);
        TenantQueryService tenantQueryService = new TenantQueryService(new StubTenantRepositoryPort(List.of(brand, retail)));

        Map<String, TenantView> results = tenantQueryService.getTenants(List.of("tenant-1", "tenant-2", "tenant-1"));

        assertEquals(2, results.size());
        assertEquals("Brand One", results.get("tenant-1").name());
        assertEquals("SUSPENDED", results.get("tenant-2").status());
    }

    @Test
    void getTenants_returnsEmptyMap_whenIdsBlankOrNull() {
        TenantQueryService tenantQueryService = new TenantQueryService(new StubTenantRepositoryPort(List.of()));

        assertTrue(tenantQueryService.getTenants(List.of()).isEmpty());
        assertTrue(tenantQueryService.getTenants(Arrays.asList((String) null)).isEmpty());
    }

    private static final class StubTenantRepositoryPort implements TenantRepositoryPort {

        private final List<Tenant> tenants;

        private StubTenantRepositoryPort(List<Tenant> tenants) {
            this.tenants = tenants;
        }

        @Override
        public Tenant save(Tenant tenant) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Tenant> findById(String tenantId) {
            return tenants.stream().filter(tenant -> tenant.tenantId().equals(tenantId)).findFirst();
        }

        @Override
        public List<Tenant> findByIds(List<String> tenantIds) {
            return tenants.stream().filter(tenant -> tenantIds.contains(tenant.tenantId())).toList();
        }

        @Override
        public Page<Tenant> findPage(TenantType type, TenantStatus status, String name, Pageable pageable) {
            throw new UnsupportedOperationException();
        }
    }
}
