package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.PermissionResult;
import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import io.attestry.userauth.application.dto.result.TenantRoleTemplateBindingResult;
import io.attestry.userauth.application.port.TemplateAdminRepositoryPort;
import io.attestry.userauth.application.usecase.membership.TemplateAdminUseCase;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.authorization.policy.TenantRoleTemplateBindingPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateAdminService implements TemplateAdminUseCase {

    private final TemplateAdminRepositoryPort templateAdminRepository;
    private final TemplateAdminCommandValidator validator;
    private final TenantRoleTemplateBindingPolicy tenantRoleTemplateBindingPolicy;
    private final Clock clock;

    public TemplateAdminService(
        TemplateAdminRepositoryPort templateAdminRepository,
        TemplateAdminCommandValidator validator,
        TenantRoleTemplateBindingPolicy tenantRoleTemplateBindingPolicy,
        Clock clock
    ) {
        this.templateAdminRepository = templateAdminRepository;
        this.validator = validator;
        this.tenantRoleTemplateBindingPolicy = tenantRoleTemplateBindingPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PermissionTemplateResult createTemplate(ActorContext actor, CreateTemplateCommand command) {
        Instant now = Instant.now(clock);
        String code = validator.normalizeTemplateCode(command.code());
        TemplateAdminRepositoryPort.PermissionTemplateView created = templateAdminRepository.createTemplate(
            code,
            validator.normalizeRequired(command.name(), "name"),
            validator.normalizeOptional(command.description()),
            null,
            actor.userId(),
            now
        );
        return toResult(created);
    }

    @Override
    @Transactional
    public PermissionResult createPermission(ActorContext actor, CreatePermissionCommand command) {
        TemplateAdminRepositoryPort.PermissionView created = templateAdminRepository.createPermission(
            validator.normalizePermissionCode(command.code()),
            validator.normalizeRequired(command.name(), "name"),
            validator.normalizeOptional(command.description()),
            validator.normalizeResourceType(command.resourceType()),
            validator.normalizeAction(command.action())
        );
        return toResult(created);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResult> listPermissions(ActorContext actor) {
        return templateAdminRepository.findAllPermissions().stream()
            .map(this::toResult)
            .toList();
    }

    @Override
    @Transactional
    public PermissionTemplateResult createTenantTemplate(ActorContext actor, String tenantId, CreateTemplateCommand command) {
        Instant now = Instant.now(clock);
        String code = validator.normalizeTemplateCode(command.code());
        TemplateAdminRepositoryPort.PermissionTemplateView created = templateAdminRepository.createTemplate(
            code,
            validator.normalizeRequired(command.name(), "name"),
            validator.normalizeOptional(command.description()),
            tenantId,
            actor.userId(),
            now
        );
        return toResult(created);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionTemplateResult> listTemplates(ActorContext actor) {
        return templateAdminRepository.findAllTemplates().stream()
            .map(this::toResult)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionTemplateResult> listTenantTemplates(ActorContext actor, String tenantId) {
        return templateAdminRepository.findTemplatesVisibleToTenant(tenantId).stream()
            .map(this::toResult)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionTemplateResult getTemplate(ActorContext actor, String templateCode) {
        return toResult(
            templateAdminRepository.findTemplateByCode(validator.normalizeTemplateCode(templateCode))
                .orElseThrow(() -> new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found"))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionTemplateResult getTenantTemplate(ActorContext actor, String tenantId, String templateCode) {
        return toResult(
            templateAdminRepository.findTemplateVisibleToTenant(validator.normalizeTemplateCode(templateCode), tenantId)
                .orElseThrow(() -> new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found"))
        );
    }

    @Override
    @Transactional
    public PermissionTemplateResult updateTemplate(ActorContext actor, String templateCode, UpdateTemplateCommand command) {
        validator.validateUpdateHasAtLeastOneField(command.name(), command.description(), command.enabled());
        TemplateAdminRepositoryPort.PermissionTemplateView updated = templateAdminRepository.updateTemplateMeta(
            validator.normalizeTemplateCode(templateCode),
            null,
            command.name() == null ? null : validator.normalizeRequired(command.name(), "name"),
            command.description() == null ? null : validator.normalizeOptional(command.description()),
            command.enabled(),
            Instant.now(clock)
        );
        return toResult(updated);
    }

    @Override
    @Transactional
    public PermissionTemplateResult updateTenantTemplate(
        ActorContext actor,
        String tenantId,
        String templateCode,
        UpdateTemplateCommand command
    ) {
        validator.validateUpdateHasAtLeastOneField(command.name(), command.description(), command.enabled());
        TemplateAdminRepositoryPort.PermissionTemplateView updated = templateAdminRepository.updateTemplateMeta(
            validator.normalizeTemplateCode(templateCode),
            tenantId,
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
        Set<String> normalizedPermissionCodes = validator.normalizePermissionCodes(command.permissionCodes());
        Set<String> updatedPermissions = templateAdminRepository.replaceTemplatePermissions(
            validator.normalizeTemplateCode(templateCode),
            null,
            normalizedPermissionCodes
        );
        return withPermissions(templateCode, updatedPermissions);
    }

    @Override
    @Transactional
    public PermissionTemplateResult replaceTenantTemplatePermissions(
        ActorContext actor,
        String tenantId,
        String templateCode,
        SetTemplatePermissionsCommand command
    ) {
        Set<String> normalizedPermissionCodes = validator.normalizePermissionCodes(command.permissionCodes());
        Set<String> updatedPermissions = templateAdminRepository.replaceTemplatePermissions(
            validator.normalizeTemplateCode(templateCode),
            tenantId,
            normalizedPermissionCodes
        );
        return withPermissions(templateCode, updatedPermissions, tenantId);
    }

    @Override
    @Transactional
    public PermissionTemplateResult addTemplatePermissions(
        ActorContext actor,
        String templateCode,
        AddTemplatePermissionsCommand command
    ) {
        Set<String> normalizedPermissionCodes = validator.normalizePermissionCodes(command.permissionCodes());
        Set<String> updatedPermissions = templateAdminRepository.addTemplatePermissions(
            validator.normalizeTemplateCode(templateCode),
            null,
            normalizedPermissionCodes
        );
        return withPermissions(templateCode, updatedPermissions);
    }

    @Override
    @Transactional
    public PermissionTemplateResult addTenantTemplatePermissions(
        ActorContext actor,
        String tenantId,
        String templateCode,
        AddTemplatePermissionsCommand command
    ) {
        Set<String> normalizedPermissionCodes = validator.normalizePermissionCodes(command.permissionCodes());
        Set<String> updatedPermissions = templateAdminRepository.addTemplatePermissions(
            validator.normalizeTemplateCode(templateCode),
            tenantId,
            normalizedPermissionCodes
        );
        return withPermissions(templateCode, updatedPermissions, tenantId);
    }

    @Override
    @Transactional
    public PermissionTemplateResult removeTemplatePermission(ActorContext actor, String templateCode, String permissionCode) {
        Set<String> updatedPermissions = templateAdminRepository.removeTemplatePermission(
            validator.normalizeTemplateCode(templateCode),
            null,
            validator.normalizePermissionCode(permissionCode)
        );
        return withPermissions(templateCode, updatedPermissions);
    }

    @Override
    @Transactional
    public PermissionTemplateResult removeTenantTemplatePermission(
        ActorContext actor,
        String tenantId,
        String templateCode,
        String permissionCode
    ) {
        Set<String> updatedPermissions = templateAdminRepository.removeTemplatePermission(
            validator.normalizeTemplateCode(templateCode),
            tenantId,
            validator.normalizePermissionCode(permissionCode)
        );
        return withPermissions(templateCode, updatedPermissions, tenantId);
    }

    @Override
    @Transactional
    public TenantRoleTemplateBindingResult bindTenantRoleTemplate(
        ActorContext actor,
        String tenantId,
        BindTenantRoleTemplateCommand command
    ) {
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
        return templateAdminRepository.findTenantRoleTemplateBindings(tenantId).stream()
            .map(this::toResult)
            .toList();
    }

    @Override
    @Transactional
    public void unbindTenantRoleTemplate(ActorContext actor, String tenantId, String roleCode, String templateCode) {
        templateAdminRepository.disableTenantRoleTemplateBinding(
            tenantId,
            validator.normalizeRoleCode(roleCode),
            validator.normalizeTemplateCode(templateCode),
            Instant.now(clock)
        );
    }

    private PermissionTemplateResult withPermissions(String templateCode, Set<String> permissionCodes) {
        return withPermissions(templateCode, permissionCodes, null);
    }

    private PermissionTemplateResult withPermissions(String templateCode, Set<String> permissionCodes, String tenantId) {
        String normalizedCode = validator.normalizeTemplateCode(templateCode);
        TemplateAdminRepositoryPort.PermissionTemplateView template = (tenantId == null
            ? templateAdminRepository.findTemplateByCode(normalizedCode)
            : templateAdminRepository.findTemplateByCodeAndTenantId(normalizedCode, tenantId))
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

    private PermissionResult toResult(TemplateAdminRepositoryPort.PermissionView view) {
        return new PermissionResult(
            view.permissionId(),
            view.code(),
            view.name(),
            view.description(),
            view.resourceType(),
            view.action(),
            view.enabled()
        );
    }
}
