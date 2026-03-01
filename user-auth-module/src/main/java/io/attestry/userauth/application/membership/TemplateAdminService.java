package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import io.attestry.userauth.application.dto.result.TenantRoleTemplateBindingResult;
import io.attestry.userauth.application.port.MembershipAdminRepositoryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.application.port.TemplateAdminRepositoryPort;
import io.attestry.userauth.application.usecase.membership.TemplateAdminUseCase;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.auth.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateAdminService implements TemplateAdminUseCase {

    private static final Set<String> ALLOWED_TENANT_ROLE_CODES = Set.of(
        RoleCodes.TENANT_OWNER,
        RoleCodes.TENANT_OPERATOR,
        RoleCodes.TENANT_STAFF
    );

    private final TemplateAdminRepositoryPort templateAdminRepository;
    private final MembershipRepositoryPort membershipRepository;
    private final MembershipAdminRepositoryPort membershipAdminRepository;
    private final Clock clock;

    public TemplateAdminService(
        TemplateAdminRepositoryPort templateAdminRepository,
        MembershipRepositoryPort membershipRepository,
        MembershipAdminRepositoryPort membershipAdminRepository,
        Clock clock
    ) {
        this.templateAdminRepository = templateAdminRepository;
        this.membershipRepository = membershipRepository;
        this.membershipAdminRepository = membershipAdminRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PermissionTemplateResult createTemplate(AuthPrincipal principal, CreateTemplateCommand command) {
        assertPlatformSuperAdmin(principal);
        Instant now = Instant.now(clock);
        String code = normalizeTemplateCode(command.code());
        TemplateAdminRepositoryPort.PermissionTemplateView created = templateAdminRepository.createTemplate(
            code,
            normalizeRequired(command.name(), "name"),
            normalizeOptional(command.description()),
            principal.userId(),
            now
        );
        return toResult(created);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionTemplateResult> listTemplates(AuthPrincipal principal) {
        assertPlatformSuperAdmin(principal);
        return templateAdminRepository.findAllTemplates().stream()
            .map(this::toResult)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionTemplateResult getTemplate(AuthPrincipal principal, String templateCode) {
        assertPlatformSuperAdmin(principal);
        return toResult(
            templateAdminRepository.findTemplateByCode(normalizeTemplateCode(templateCode))
                .orElseThrow(() -> new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found"))
        );
    }

    @Override
    @Transactional
    public PermissionTemplateResult updateTemplate(AuthPrincipal principal, String templateCode, UpdateTemplateCommand command) {
        assertPlatformSuperAdmin(principal);
        if (command.name() == null && command.description() == null && command.enabled() == null) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "At least one field must be provided");
        }
        TemplateAdminRepositoryPort.PermissionTemplateView updated = templateAdminRepository.updateTemplateMeta(
            normalizeTemplateCode(templateCode),
            command.name() == null ? null : normalizeRequired(command.name(), "name"),
            command.description() == null ? null : normalizeOptional(command.description()),
            command.enabled(),
            Instant.now(clock)
        );
        return toResult(updated);
    }

    @Override
    @Transactional
    public PermissionTemplateResult replaceTemplatePermissions(
        AuthPrincipal principal,
        String templateCode,
        SetTemplatePermissionsCommand command
    ) {
        assertPlatformSuperAdmin(principal);
        Set<String> normalizedPermissionCodes = normalizePermissionCodes(command.permissionCodes());
        Set<String> updatedPermissions = templateAdminRepository.replaceTemplatePermissions(
            normalizeTemplateCode(templateCode),
            normalizedPermissionCodes
        );
        return withPermissions(templateCode, updatedPermissions);
    }

    @Override
    @Transactional
    public PermissionTemplateResult addTemplatePermissions(
        AuthPrincipal principal,
        String templateCode,
        AddTemplatePermissionsCommand command
    ) {
        assertPlatformSuperAdmin(principal);
        Set<String> normalizedPermissionCodes = normalizePermissionCodes(command.permissionCodes());
        Set<String> updatedPermissions = templateAdminRepository.addTemplatePermissions(
            normalizeTemplateCode(templateCode),
            normalizedPermissionCodes
        );
        return withPermissions(templateCode, updatedPermissions);
    }

    @Override
    @Transactional
    public PermissionTemplateResult removeTemplatePermission(AuthPrincipal principal, String templateCode, String permissionCode) {
        assertPlatformSuperAdmin(principal);
        Set<String> updatedPermissions = templateAdminRepository.removeTemplatePermission(
            normalizeTemplateCode(templateCode),
            normalizePermissionCode(permissionCode)
        );
        return withPermissions(templateCode, updatedPermissions);
    }

    @Override
    @Transactional
    public TenantRoleTemplateBindingResult bindTenantRoleTemplate(
        AuthPrincipal principal,
        String tenantId,
        BindTenantRoleTemplateCommand command
    ) {
        assertPlatformSuperAdmin(principal);
        String normalizedRoleCode = normalizeRoleCode(command.roleCode());
        if (!ALLOWED_TENANT_ROLE_CODES.contains(normalizedRoleCode)) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "roleCode must be one of TENANT_OWNER, TENANT_OPERATOR, TENANT_STAFF");
        }
        TemplateAdminRepositoryPort.TenantRoleTemplateBindingView binding = templateAdminRepository.bindTemplateToTenantRole(
            tenantId,
            normalizedRoleCode,
            normalizeTemplateCode(command.templateCode()),
            principal.userId(),
            Instant.now(clock)
        );
        return toResult(binding);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantRoleTemplateBindingResult> listTenantRoleTemplateBindings(AuthPrincipal principal, String tenantId) {
        assertPlatformSuperAdmin(principal);
        return templateAdminRepository.findTenantRoleTemplateBindings(tenantId).stream()
            .map(this::toResult)
            .toList();
    }

    @Override
    @Transactional
    public void unbindTenantRoleTemplate(AuthPrincipal principal, String tenantId, String roleCode, String templateCode) {
        assertPlatformSuperAdmin(principal);
        templateAdminRepository.disableTenantRoleTemplateBinding(
            tenantId,
            normalizeRoleCode(roleCode),
            normalizeTemplateCode(templateCode),
            Instant.now(clock)
        );
    }

    private void assertPlatformSuperAdmin(AuthPrincipal principal) {
        List<Membership> memberships = membershipRepository.findByUserId(principal.userId());
        boolean allowed = memberships.stream()
            .map(Membership::membershipId)
            .map(membershipAdminRepository::findRoleCodesByMembershipId)
            .anyMatch(roleCodes -> roleCodes.contains(RoleCodes.PLATFORM_SUPER_ADMIN));
        if (!allowed) {
            throw new DomainException(ErrorCode.FORBIDDEN_SCOPE, "PLATFORM_SUPER_ADMIN role is required");
        }
    }

    private PermissionTemplateResult withPermissions(String templateCode, Set<String> permissionCodes) {
        TemplateAdminRepositoryPort.PermissionTemplateView template = templateAdminRepository.findTemplateByCode(normalizeTemplateCode(templateCode))
            .orElseThrow(() -> new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found"));
        return new PermissionTemplateResult(
            template.templateId(),
            template.code(),
            template.name(),
            template.description(),
            template.enabled(),
            permissionCodes.stream().sorted().toList()
        );
    }

    private String normalizeTemplateCode(String code) {
        return normalizeRequired(code, "templateCode").toUpperCase(Locale.ROOT);
    }

    private String normalizeRoleCode(String code) {
        return normalizeRequired(code, "roleCode").toUpperCase(Locale.ROOT);
    }

    private Set<String> normalizePermissionCodes(List<String> permissionCodes) {
        if (permissionCodes == null) {
            return Set.of();
        }
        return permissionCodes.stream()
            .map(this::normalizePermissionCode)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizePermissionCode(String code) {
        return normalizeRequired(code, "permissionCode").toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null ? null : value.trim();
    }

    private PermissionTemplateResult toResult(TemplateAdminRepositoryPort.PermissionTemplateView view) {
        return new PermissionTemplateResult(
            view.templateId(),
            view.code(),
            view.name(),
            view.description(),
            view.enabled(),
            view.permissionCodes().stream().sorted().toList()
        );
    }

    private TenantRoleTemplateBindingResult toResult(TemplateAdminRepositoryPort.TenantRoleTemplateBindingView view) {
        return new TenantRoleTemplateBindingResult(
            view.bindingId(),
            view.tenantId(),
            view.roleCode(),
            view.templateCode(),
            view.enabled()
        );
    }
}
