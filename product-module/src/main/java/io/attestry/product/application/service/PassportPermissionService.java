package io.attestry.product.application.service;

import io.attestry.product.application.dto.command.GrantCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.result.GrantResult;
import io.attestry.product.application.port.PassportPermissionPort;
import io.attestry.product.application.port.PassportPort;
import io.attestry.product.application.port.ProductAuthorizationPort;
import io.attestry.product.application.usecase.PassportPermissionUseCase;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.permission.model.PassportPermission;
import io.attestry.product.domain.service.UuidV7Generator;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassportPermissionService implements PassportPermissionUseCase {

    private final PassportPermissionPort permissionPort;
    private final PassportPort passportPort;
    private final ProductAuthorizationPort productAuthorizationPort;
    private final UuidV7Generator uuidV7Generator;
    private final Clock clock;

    public PassportPermissionService(
        PassportPermissionPort permissionPort,
        PassportPort passportPort,
        ProductAuthorizationPort productAuthorizationPort,
        UuidV7Generator uuidV7Generator,
        Clock clock
    ) {
        this.permissionPort = permissionPort;
        this.passportPort = passportPort;
        this.productAuthorizationPort = productAuthorizationPort;
        this.uuidV7Generator = uuidV7Generator;
        this.clock = clock;
    }

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
