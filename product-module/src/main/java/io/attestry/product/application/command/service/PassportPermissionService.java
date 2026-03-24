package io.attestry.product.application.command.service;

import io.attestry.product.application.command.model.GrantCommand;
import io.attestry.product.application.common.ProductActor;
import io.attestry.product.application.command.result.GrantResult;
import io.attestry.product.application.port.permission.PassportPermissionPort;
import io.attestry.product.application.port.passport.PassportPort;
import io.attestry.product.application.port.auth.ProductAuthorizationPort;
import io.attestry.product.application.command.usecase.PassportPermissionUseCase;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.permission.model.PassportPermission;
import io.attestry.product.domain.service.UuidV7Generator;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PassportPermissionService implements PassportPermissionUseCase {

    private final PassportPermissionPort permissionPort;
    private final PassportPort passportPort;
    private final ProductAuthorizationPort productAuthorizationPort;
    private final UuidV7Generator uuidV7Generator;
    private final Clock clock;

    @Override
    @Transactional
    public GrantResult grantPermission(ProductActor actor, GrantCommand command) {
        productAuthorizationPort.assertPassportPermissionGrantAllowed(actor);

        passportPort.findById(command.passportId())
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
        permission = permissionPort.save(permission);

        return new GrantResult(
            permission.getPermissionId(),
            permission.getPassportId(),
            permission.getSellerTenantId(),
            permission.getScope().name()
        );
    }

    @Override
    @Transactional
    public void revokePermission(ProductActor actor, String permissionId) {
        productAuthorizationPort.assertPassportPermissionGrantAllowed(actor);

        PassportPermission permission = permissionPort.findById(permissionId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Permission not found: " + permissionId));

        permission.revoke();
        permissionPort.save(permission);
    }

    @Override
    @Transactional
    public void suspendPermission(ProductActor actor, String permissionId) {
        productAuthorizationPort.assertPassportPermissionGrantAllowed(actor);

        PassportPermission permission = permissionPort.findById(permissionId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Permission not found: " + permissionId));

        permission.suspend();
        permissionPort.save(permission);
    }

}
