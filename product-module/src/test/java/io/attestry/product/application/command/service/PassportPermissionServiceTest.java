package io.attestry.product.application.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.product.application.command.model.GrantCommand;
import io.attestry.product.application.command.result.GrantResult;
import io.attestry.product.application.common.ProductActor;
import io.attestry.product.application.port.auth.ProductAuthorizationPort;
import io.attestry.product.application.port.passport.PassportPort;
import io.attestry.product.application.port.permission.PassportPermissionPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.permission.model.PassportPermission;
import io.attestry.product.domain.permission.model.PermissionScope;
import io.attestry.product.domain.service.UuidV7Generator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassportPermissionServiceTest {

    @Mock PassportPermissionPort permissionPort;
    @Mock PassportPort passportPort;
    @Mock ProductAuthorizationPort productAuthorizationPort;
    @Mock UuidV7Generator uuidV7Generator;

    private final Clock clock = Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.UTC);

    private PassportPermissionService service;

    private static final ProductActor ACTOR = new ProductActor(
        "user-1", "brand-tenant", Set.of("BRAND_PERMISSION_GRANT"), false
    );

    @BeforeEach
    void setUp() {
        service = new PassportPermissionService(
            permissionPort, passportPort, productAuthorizationPort, uuidV7Generator, clock
        );
    }

    // ── grant ──

    @Test
    void grant_success() {
        GrantCommand command = new GrantCommand(
            "p1", "seller-tenant", PermissionScope.RETAIL_SALE,
            Instant.parse("2026-07-01T00:00:00Z")
        );

        doNothing().when(productAuthorizationPort).assertPassportPermissionGrantAllowed(ACTOR);
        when(passportPort.findById("p1")).thenReturn(Optional.of(org.mockito.Mockito.mock(ProductPassport.class)));
        when(uuidV7Generator.nextId()).thenReturn("perm-1");
        when(permissionPort.save(any(PassportPermission.class))).thenAnswer(inv -> inv.getArgument(0));

        GrantResult result = service.grantPermission(ACTOR, command);

        assertEquals("perm-1", result.permissionId());
        assertEquals("p1", result.passportId());
        assertEquals("seller-tenant", result.sellerTenantId());
        assertEquals("RETAIL_SALE", result.scope());
    }

    @Test
    void grant_passportNotFound_throws() {
        GrantCommand command = new GrantCommand(
            "missing", "seller-tenant", PermissionScope.RETAIL_SALE, null
        );

        doNothing().when(productAuthorizationPort).assertPassportPermissionGrantAllowed(ACTOR);
        when(passportPort.findById("missing")).thenReturn(Optional.empty());

        ProductDomainException ex = assertThrows(ProductDomainException.class,
            () -> service.grantPermission(ACTOR, command));
        assertEquals(ProductErrorCode.ASSET_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void grant_forbidden_throws() {
        GrantCommand command = new GrantCommand(
            "p1", "seller-tenant", PermissionScope.RETAIL_SALE, null
        );

        doThrow(new ProductDomainException(ProductErrorCode.FORBIDDEN_MINT, "Forbidden"))
            .when(productAuthorizationPort).assertPassportPermissionGrantAllowed(ACTOR);

        ProductDomainException ex = assertThrows(ProductDomainException.class,
            () -> service.grantPermission(ACTOR, command));
        assertEquals(ProductErrorCode.FORBIDDEN_MINT, ex.getErrorCode());
    }

    // ── revoke ──

    @Test
    void revoke_success() {
        PassportPermission permission = PassportPermission.grant(
            "perm-1", "p1", "seller-tenant", PermissionScope.RETAIL_SALE,
            Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z")
        );

        doNothing().when(productAuthorizationPort).assertPassportPermissionGrantAllowed(ACTOR);
        when(permissionPort.findById("perm-1")).thenReturn(Optional.of(permission));

        service.revokePermission(ACTOR, "perm-1");

        verify(permissionPort).save(any(PassportPermission.class));
    }

    @Test
    void revoke_notFound_throws() {
        doNothing().when(productAuthorizationPort).assertPassportPermissionGrantAllowed(ACTOR);
        when(permissionPort.findById("missing")).thenReturn(Optional.empty());

        ProductDomainException ex = assertThrows(ProductDomainException.class,
            () -> service.revokePermission(ACTOR, "missing"));
        assertEquals(ProductErrorCode.ASSET_NOT_FOUND, ex.getErrorCode());
    }

    // ── suspend ──

    @Test
    void suspend_success() {
        PassportPermission permission = PassportPermission.grant(
            "perm-1", "p1", "seller-tenant", PermissionScope.RETAIL_SALE,
            Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z")
        );

        doNothing().when(productAuthorizationPort).assertPassportPermissionGrantAllowed(ACTOR);
        when(permissionPort.findById("perm-1")).thenReturn(Optional.of(permission));

        service.suspendPermission(ACTOR, "perm-1");

        verify(permissionPort).save(any(PassportPermission.class));
    }

    @Test
    void suspend_notFound_throws() {
        doNothing().when(productAuthorizationPort).assertPassportPermissionGrantAllowed(ACTOR);
        when(permissionPort.findById("missing")).thenReturn(Optional.empty());

        ProductDomainException ex = assertThrows(ProductDomainException.class,
            () -> service.suspendPermission(ACTOR, "missing"));
        assertEquals(ProductErrorCode.ASSET_NOT_FOUND, ex.getErrorCode());
    }
}
