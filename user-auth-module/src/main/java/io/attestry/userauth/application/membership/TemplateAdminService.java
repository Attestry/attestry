package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import io.attestry.userauth.application.dto.result.TenantRoleTemplateBindingResult;
import io.attestry.userauth.application.port.TemplateAdminRepositoryPort;
import io.attestry.userauth.application.usecase.membership.TemplateAdminUseCase;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.policy.TenantRoleTemplateBindingPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateAdminService implements TemplateAdminUseCase {

    private final TemplateAdminRepositoryPort templateAdminRepository;
    private final PlatformSuperAdminAuthorizationChecker authorizationChecker;
    private final TemplateAdminCommandValidator validator;
    private final TenantRoleTemplateBindingPolicy tenantRoleTemplateBindingPolicy;
    private final Clock clock;

    public TemplateAdminService(
        TemplateAdminRepositoryPort templateAdminRepository,
        PlatformSuperAdminAuthorizationChecker authorizationChecker,
        TemplateAdminCommandValidator validator,
        TenantRoleTemplateBindingPolicy tenantRoleTemplateBindingPolicy,
        Clock clock
    ) {
        this.templateAdminRepository = templateAdminRepository;
        this.authorizationChecker = authorizationChecker;
        this.validator = validator;
        this.tenantRoleTemplateBindingPolicy = tenantRoleTemplateBindingPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PermissionTemplateResult createTemplate(ActorContext actor, CreateTemplateCommand command) {
        authorizationChecker.assertPlatformSuperAdmin(actor.userId());
        Instant now = Instant.now(clock);
        String code = validator.normalizeTemplateCode(command.code());
        TemplateAdminRepositoryPort.PermissionTemplateView created = templateAdminRepository.createTemplate(
            code,
            validator.normalizeRequired(command.name(), "name"),
            validator.normalizeOptional(command.description()),
            actor.userId(),
            now
        );
        return toResult(created);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionTemplateResult> listTemplates(ActorContext actor) {
        authorizationChecker.assertPlatformSuperAdmin(actor.userId());
        return templateAdminRepository.findAllTemplates().stream()
            .map(this::toResult)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionTemplateResult getTemplate(ActorContext actor, String templateCode) {
        authorizationChecker.assertPlatformSuperAdmin(actor.userId());
        return toResult(
            templateAdminRepository.findTemplateByCode(validator.normalizeTemplateCode(templateCode))
                .orElseThrow(() -> new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found"))
        );
    }

    @Override
    @Transactional
    public PermissionTemplateResult updateTemplate(ActorContext actor, String templateCode, UpdateTemplateCommand command) {
        authorizationChecker.assertPlatformSuperAdmin(actor.userId());
        validator.validateUpdateHasAtLeastOneField(command.name(), command.description(), command.enabled());
        TemplateAdminRepositoryPort.PermissionTemplateView updated = templateAdminRepository.updateTemplateMeta(
            validator.normalizeTemplateCode(templateCode),
            command.name() == null ? null : validator.normalizeRequired(command.name(), "name"),
            command.description() == null ? null : validator.normalizeOptional(command.description()),
            command.enabled(),
            Instant.now(clock)
        );
        return toResult(updated);
    }

    @Override
    @Transactional
    public PermissionTemplateResult replaceTemplatePermissions(
        ActorContext actor,
        String templateCode,
        SetTemplatePermissionsCommand command
    ) {
        authorizationChecker.assertPlatformSuperAdmin(actor.userId());
        Set<String> normalizedPermissionCodes = validator.normalizePermissionCodes(command.permissionCodes());
        Set<String> updatedPermissions = templateAdminRepository.replaceTemplatePermissions(
            validator.normalizeTemplateCode(templateCode),
            normalizedPermissionCodes
        );
        return withPermissions(templateCode, updatedPermissions);
    }

    @Override
    @Transactional
    public PermissionTemplateResult addTemplatePermissions(
        ActorContext actor,
        String templateCode,
        AddTemplatePermissionsCommand command
    ) {
        authorizationChecker.assertPlatformSuperAdmin(actor.userId());
        Set<String> normalizedPermissionCodes = validator.normalizePermissionCodes(command.permissionCodes());
        Set<String> updatedPermissions = templateAdminRepository.addTemplatePermissions(
            validator.normalizeTemplateCode(templateCode),
            normalizedPermissionCodes
        );
        return withPermissions(templateCode, updatedPermissions);
    }

    @Override
    @Transactional
    public PermissionTemplateResult removeTemplatePermission(ActorContext actor, String templateCode, String permissionCode) {
        authorizationChecker.assertPlatformSuperAdmin(actor.userId());
        Set<String> updatedPermissions = templateAdminRepository.removeTemplatePermission(
            validator.normalizeTemplateCode(templateCode),
            validator.normalizePermissionCode(permissionCode)
        );
        return withPermissions(templateCode, updatedPermissions);
    }

    @Override
    @Transactional
    public TenantRoleTemplateBindingResult bindTenantRoleTemplate(
        ActorContext actor,
        String tenantId,
        BindTenantRoleTemplateCommand command
    ) {
        authorizationChecker.assertPlatformSuperAdmin(actor.userId());
        String normalizedRoleCode = validator.normalizeRoleCode(command.roleCode());
        tenantRoleTemplateBindingPolicy.assertAllowedRoleCode(normalizedRoleCode);
        TemplateAdminRepositoryPort.TenantRoleTemplateBindingView binding = templateAdminRepository.bindTemplateToTenantRole(
            tenantId,
            normalizedRoleCode,
            validator.normalizeTemplateCode(command.templateCode()),
            actor.userId(),
            Instant.now(clock)
        );
        return toResult(binding);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantRoleTemplateBindingResult> listTenantRoleTemplateBindings(ActorContext actor, String tenantId) {
        authorizationChecker.assertPlatformSuperAdmin(actor.userId());
        return templateAdminRepository.findTenantRoleTemplateBindings(tenantId).stream()
            .map(this::toResult)
            .toList();
    }

    @Override
    @Transactional
    public void unbindTenantRoleTemplate(ActorContext actor, String tenantId, String roleCode, String templateCode) {
        authorizationChecker.assertPlatformSuperAdmin(actor.userId());
        templateAdminRepository.disableTenantRoleTemplateBinding(
            tenantId,
            validator.normalizeRoleCode(roleCode),
            validator.normalizeTemplateCode(templateCode),
            Instant.now(clock)
        );
    }

    private PermissionTemplateResult withPermissions(String templateCode, Set<String> permissionCodes) {
        TemplateAdminRepositoryPort.PermissionTemplateView template = templateAdminRepository.findTemplateByCode(validator.normalizeTemplateCode(templateCode))
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
