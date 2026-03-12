package io.attestry.userauth.application.onboarding;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.port.MembershipProvisioningRepositoryPort;
import io.attestry.userauth.application.port.TemplateAdminRepositoryPort;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.authorization.policy.SystemPermissionTemplateCatalog;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.organization.model.Tenant;
import io.attestry.userauth.domain.organization.model.TenantType;
import io.attestry.userauth.domain.organization.repository.TenantRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnboardingProvisioningServiceTest {

    private final InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
    private final NoopMembershipProvisioningRepository membershipProvisioningRepository =
        new NoopMembershipProvisioningRepository();
    private final RecordingTemplateAdminRepository templateRepository = new RecordingTemplateAdminRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-06T00:00:00Z"), ZoneOffset.UTC);

    private final OnboardingProvisioningService service = new OnboardingProvisioningService(
        tenantRepository,
        membershipProvisioningRepository,
        templateRepository,
        clock
    );

    @Test
    void provisionBrandShouldBindBrandWorkTemplateToOperator() {
        OnboardingProvisioningService.ProvisioningResult result = service.provision(
            TenantType.BRAND,
            "user-1",
            "brand-org",
            "KR",
            null,
            "admin-1"
        );

        assertHasBinding(result.tenantId(), RoleCodes.TENANT_OWNER, SystemPermissionTemplateCatalog.TEMPLATE_TENANT_OWNER_CORE);
        assertHasBinding(result.tenantId(), RoleCodes.TENANT_OWNER, SystemPermissionTemplateCatalog.TEMPLATE_TENANT_READ_ONLY);
        assertHasBinding(result.tenantId(), RoleCodes.TENANT_OPERATOR, SystemPermissionTemplateCatalog.TEMPLATE_TENANT_READ_ONLY);
        assertHasBinding(result.tenantId(), RoleCodes.TENANT_OPERATOR, SystemPermissionTemplateCatalog.TEMPLATE_BRAND_WORK);
        assertHasBinding(result.tenantId(), RoleCodes.TENANT_STAFF, SystemPermissionTemplateCatalog.TEMPLATE_TENANT_READ_ONLY);
    }

    @Test
    void provisionRetailShouldBindRetailWorkTemplateToOperator() {
        OnboardingProvisioningService.ProvisioningResult result = service.provision(
            TenantType.RETAIL,
            "user-2",
            "retail-org",
            "KR",
            null,
            "admin-1"
        );

        assertHasBinding(result.tenantId(), RoleCodes.TENANT_OPERATOR, SystemPermissionTemplateCatalog.TEMPLATE_RETAIL_WORK);
    }

    @Test
    void provisionServiceShouldBindServiceWorkTemplateToOperator() {
        OnboardingProvisioningService.ProvisioningResult result = service.provision(
            TenantType.SERVICE,
            "user-3",
            "service-org",
            "KR",
            "서울시 강남구 테헤란로 1",
            "admin-1"
        );

        assertHasBinding(result.tenantId(), RoleCodes.TENANT_OPERATOR, SystemPermissionTemplateCatalog.TEMPLATE_SERVICE_WORK);
    }

    private void assertHasBinding(String tenantId, String roleCode, String templateCode) {
        assertTrue(
            templateRepository.findTenantRoleTemplateBindings(tenantId).stream()
                .anyMatch(binding -> binding.roleCode().equals(roleCode) && binding.templateCode().equals(templateCode) && binding.enabled()),
            "missing binding: tenantId=%s roleCode=%s templateCode=%s".formatted(tenantId, roleCode, templateCode)
        );
    }

    private static class InMemoryTenantRepository implements TenantRepository {
        private final Map<String, Tenant> byTenantId = new HashMap<>();

        @Override
        public Tenant save(Tenant tenant) {
            byTenantId.put(tenant.tenantId(), tenant);
            return tenant;
        }

        @Override
        public Optional<Tenant> findById(String tenantId) {
            return Optional.ofNullable(byTenantId.get(tenantId));
        }
    }

    private static class NoopMembershipProvisioningRepository implements MembershipProvisioningRepositoryPort {
        @Override
        public Membership save(Membership membership) {
            return membership;
        }

        @Override
        public void assignRole(String membershipId, String roleCode, String assignedByUserId) {
            // no-op for this test
        }
    }

    private static class RecordingTemplateAdminRepository implements TemplateAdminRepositoryPort {
        private final List<TenantRoleTemplateBindingView> bindings = new ArrayList<>();

        @Override
        public TenantRoleTemplateBindingView bindTemplateToTenantRole(
            String tenantId,
            String roleCode,
            String templateCode,
            String actorUserId,
            Instant now
        ) {
            TenantRoleTemplateBindingView view = new TenantRoleTemplateBindingView(
                UUID.randomUUID().toString(),
                tenantId,
                roleCode,
                templateCode,
                true
            );
            bindings.add(view);
            return view;
        }

        @Override
        public List<TenantRoleTemplateBindingView> findTenantRoleTemplateBindings(String tenantId) {
            return bindings.stream()
                .filter(binding -> binding.tenantId().equals(tenantId))
                .toList();
        }

        @Override
        public Optional<PermissionTemplateView> findTemplateByCode(String templateCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PermissionTemplateView> findTemplateByCodeAndTenantId(String templateCode, String tenantId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PermissionTemplateView> findTemplateVisibleToTenant(String templateCode, String tenantId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PermissionTemplateView> findAllTemplates() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PermissionTemplateView> findTemplatesVisibleToTenant(String tenantId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PermissionTemplateView createTemplate(
            String code,
            String name,
            String description,
            String tenantId,
            String actorUserId,
            Instant now
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PermissionTemplateView updateTemplateMeta(
            String code,
            String tenantId,
            String name,
            String description,
            Boolean enabled,
            Instant now
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PermissionView createPermission(
            String code,
            String name,
            String description,
            String resourceType,
            String action
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PermissionView> findAllPermissions() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> replaceTemplatePermissions(String templateCode, String tenantId, Set<String> permissionCodes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> addTemplatePermissions(String templateCode, String tenantId, Set<String> permissionCodes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> removeTemplatePermission(String templateCode, String tenantId, String permissionCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void disableTenantRoleTemplateBinding(String tenantId, String roleCode, String templateCode, Instant now) {
            throw new UnsupportedOperationException();
        }
    }
}
