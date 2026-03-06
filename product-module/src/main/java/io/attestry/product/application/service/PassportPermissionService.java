package io.attestry.product.application.service;

import io.attestry.product.application.usecase.PassportPermissionUseCase;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.permission.model.PassportPermission;
import io.attestry.product.domain.permission.repository.PassportPermissionRepository;
import io.attestry.product.domain.passport.repository.PassportRepository;
import io.attestry.product.domain.service.UuidV7Generator;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassportPermissionService implements PassportPermissionUseCase {

    private final PassportPermissionRepository permissionRepository;
    private final PassportRepository passportRepository;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final UuidV7Generator uuidV7Generator;
    private final Clock clock;

    public PassportPermissionService(
        PassportPermissionRepository permissionRepository,
        PassportRepository passportRepository,
        EvaluateAuthorizationUseCase evaluateAuthorizationUseCase,
        UuidV7Generator uuidV7Generator,
        Clock clock
    ) {
        this.permissionRepository = permissionRepository;
        this.passportRepository = passportRepository;
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
        this.uuidV7Generator = uuidV7Generator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public GrantResult grantPermission(ActorContext actor, GrantCommand command) {
        assertPermissionGrantScope(actor);

        passportRepository.findById(command.passportId())
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found: " + command.passportId()));

        Instant now = Instant.now(clock);
        PassportPermission permission = PassportPermission.grant(
            uuidV7Generator.nextId(),
            command.passportId(),
            command.sellerTenantId(),
            command.scope(),
            command.expiresAt(),
            now
        );
        permission = permissionRepository.save(permission);

        return new GrantResult(
            permission.getPermissionId(),
            permission.getPassportId(),
            permission.getSellerTenantId(),
            permission.getScope().name()
        );
    }

    @Override
    @Transactional
    public void revokePermission(ActorContext actor, String permissionId) {
        assertPermissionGrantScope(actor);

        PassportPermission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Permission not found: " + permissionId));

        permission.revoke();
        permissionRepository.save(permission);
    }

    @Override
    @Transactional
    public void suspendPermission(ActorContext actor, String permissionId) {
        assertPermissionGrantScope(actor);

        PassportPermission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Permission not found: " + permissionId));

        permission.suspend();
        permissionRepository.save(permission);
    }

    private void assertPermissionGrantScope(ActorContext actor) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            actor,
            new AuthzEvaluateCommand(
                actor.tenantId(),
                PermissionCodes.PASSPORT_PERMISSION_GRANT,
                null,
                PolicyDecisionMode.LIVE_RECHECK
            )
        );
        if (!decision.allowed()) {
            throw new ProductDomainException(ProductErrorCode.FORBIDDEN_VOID,
                "PASSPORT_PERMISSION_GRANT scope is required");
        }
    }
}
