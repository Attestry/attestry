package io.attestry.product.application.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.product.application.command.internal.ProductVoidAccessPolicy;
import io.attestry.product.application.command.internal.ProductVoidExecutor;
import io.attestry.product.application.command.internal.VoidExecution;
import io.attestry.product.application.command.model.VoidCommand;
import io.attestry.product.application.command.result.VoidResult;
import io.attestry.product.application.common.ProductActor;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.passport.model.AssetState;
import io.attestry.product.domain.passport.model.ProductAsset;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.passport.model.VoidReason;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductVoidServiceTest {

    @Mock ProductVoidAccessPolicy voidAccessPolicy;
    @Mock ProductVoidExecutor voidExecutor;

    private ProductVoidService service;

    @BeforeEach
    void setUp() {
        service = new ProductVoidService(voidAccessPolicy, voidExecutor);
    }

    @Test
    void voidAsset_success() {
        ProductActor actor = new ProductActor("user-1", "brand-tenant", Set.of("BRAND_VOID"), false);
        VoidCommand command = new VoidCommand("brand-tenant", "passport-1", "COUNTERFEIT_DETECTED", "some note");

        ProductPassport passport = mock(ProductPassport.class);
        ProductAsset asset = mock(ProductAsset.class);
        when(passport.getAsset()).thenReturn(asset);
        when(asset.getAssetId()).thenReturn("asset-1");
        when(asset.getAssetState()).thenReturn(AssetState.VOIDED);

        VoidExecution execution = new VoidExecution(passport, "outbox-1");
        when(voidExecutor.execute("passport-1", VoidReason.COUNTERFEIT_DETECTED, "some note", "user-1"))
            .thenReturn(execution);

        VoidResult result = service.voidAsset(actor, command);

        assertEquals("asset-1", result.assetId());
        assertEquals("VOIDED", result.assetState());
        assertEquals("outbox-1", result.outboxEventId());

        verify(voidAccessPolicy).assertVoidAllowed(actor, "brand-tenant", "passport-1");
    }

    @Test
    void voidAsset_failsWhenAccessDenied() {
        ProductActor actor = new ProductActor("user-1", "brand-tenant", Set.of("BRAND_VOID"), false);
        VoidCommand command = new VoidCommand("brand-tenant", "passport-1", "COUNTERFEIT_DETECTED", "note");

        ProductDomainException forbidden = new ProductDomainException(
            ProductErrorCode.FORBIDDEN_VOID, "Forbidden void"
        );
        doThrow(forbidden).when(voidAccessPolicy).assertVoidAllowed(actor, "brand-tenant", "passport-1");

        ProductDomainException ex = assertThrows(ProductDomainException.class, () -> service.voidAsset(actor, command));
        assertEquals(ProductErrorCode.FORBIDDEN_VOID, ex.getErrorCode());
    }

    @Test
    void voidAsset_failsWithInvalidReason() {
        ProductActor actor = new ProductActor("user-1", "brand-tenant", Set.of("BRAND_VOID"), false);
        VoidCommand command = new VoidCommand("brand-tenant", "passport-1", "NOT_A_VALID_REASON", "note");

        ProductDomainException ex = assertThrows(ProductDomainException.class, () -> service.voidAsset(actor, command));
        assertEquals(ProductErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void voidAsset_systemActor_success() {
        String passportId = "passport-1";
        VoidReason reason = VoidReason.LEGAL_ISSUE;
        String note = "system void note";

        service.voidAsset(passportId, reason, note);

        verify(voidExecutor).execute(passportId, reason, note, "SYSTEM");
    }
}
