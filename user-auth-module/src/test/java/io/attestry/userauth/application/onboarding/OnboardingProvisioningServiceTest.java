package io.attestry.userauth.application.onboarding;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.onboarding.command.OnboardingProvisioningService;
import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.application.port.tenant.TenantRepositoryPort;
import io.attestry.userauth.application.port.template.TenantRoleTemplateBindingPort;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.authorization.policy.SystemPermissionTemplateCatalog;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.tenant.model.Tenant;
import io.attestry.userauth.domain.tenant.model.TenantType;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class OnboardingProvisioningServiceTest {

    private final InMemoryTenantRepository tenantRepository = new InMemoryTenantRepository();
    private final NoopMembershipRepository membershipRepository = new NoopMembershipRepository();
    private final RecordingTenantRoleTemplateBindingPort templateRepository = new RecordingTenantRoleTemplateBindingPort();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-06T00:00:00Z"), ZoneOffset.UTC);

    private final OnboardingProvisioningService service = new OnboardingProvisioningService(
        tenantRepository,
        membershipRepository,
        templateRepository,
        clock,
        (EntityManager) java.lang.reflect.Proxy.newProxyInstance(
            EntityManager.class.getClassLoader(),
            new Class[]{EntityManager.class},
            (proxy, method, args) -> null
        )
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

    private static class InMemoryTenantRepository implements TenantRepositoryPort {
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

        @Override
        public Page<Tenant> findPage(
                io.attestry.userauth.domain.tenant.model.TenantType type,
                io.attestry.userauth.domain.tenant.model.TenantStatus status,
                String name,
                Pageable pageable) {
            return new PageImpl<>(List.of());
        }
    }

    private static class NoopMembershipRepository implements MembershipPort {
        @Override
        public Optional<Membership> findById(String membershipId) { return Optional.empty(); }

        @Override
        public Membership save(Membership membership) { return membership; }

        @Override
        public List<Membership> findByUserId(String userId) { return List.of(); }

        @Override
        public Optional<Membership> findByUserIdAndTenantId(String userId, String tenantId) { return Optional.empty(); }

        @Override
        public List<Membership> findByTenantId(String tenantId) { return List.of(); }

        @Override
        public List<Membership> findMembershipsByTenantId(String tenantId) { return List.of(); }

        @Override
        public Optional<Membership> findMembershipById(String membershipId) { return Optional.empty(); }

        @Override
        public Membership updateMembership(
                String tenantId,
                String membershipId,
                io.attestry.userauth.domain.membership.model.MembershipRole role,
                io.attestry.userauth.domain.membership.model.MembershipStatus status) {
            return null;
        }

        @Override
        public void assignRole(String membershipId, String roleCode, String assignedByUserId) {
            // no-op for this test
        }

        @Override
        public void deletePermissionOverrides(String membershipId, Set<String> permissionCodes) {}

        @Override
        public Set<String> applyPermissionTemplateToMembership(String membershipId, String templateCode, String reason, String actorUserId, Instant now) { return Set.of(); }

        @Override
        public Set<String> revokePermissionTemplateFromMembership(String membershipId, String templateCode) { return Set.of(); }
    }

    private static class RecordingTenantRoleTemplateBindingPort implements TenantRoleTemplateBindingPort {
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
        public void disableTenantRoleTemplateBinding(String tenantId, String roleCode, String templateCode, Instant now) {
            throw new UnsupportedOperationException();
        }
    }
}
