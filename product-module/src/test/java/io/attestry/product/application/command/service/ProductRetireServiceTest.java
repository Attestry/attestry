package io.attestry.product.application.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.product.application.command.internal.ProductRetireAccessPolicy;
import io.attestry.product.application.command.internal.ProductRetireExecutor;
import io.attestry.product.application.command.internal.RetireExecution;
import io.attestry.product.application.command.model.RetireCommand;
import io.attestry.product.application.command.result.RetireResult;
import io.attestry.product.application.common.ProductActor;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.passport.model.AssetState;
import io.attestry.product.domain.passport.model.ProductAsset;
import io.attestry.product.domain.passport.model.ProductPassport;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductRetireServiceTest {

    @Mock ProductRetireAccessPolicy retireAccessPolicy;
    @Mock ProductRetireExecutor retireExecutor;

    private ProductRetireService service;

    @BeforeEach
    void setUp() {
        service = new ProductRetireService(retireAccessPolicy, retireExecutor);
    }

    @Test
    void retire_success() {
        ProductActor actor = new ProductActor("user-1", "brand-tenant", Set.of("OWNER_RETIRE"), false);
        RetireCommand command = new RetireCommand("passport-1");

        ProductPassport passport = mock(ProductPassport.class);
        ProductAsset asset = mock(ProductAsset.class);
        when(passport.getAsset()).thenReturn(asset);
        when(asset.getAssetId()).thenReturn("asset-1");
        when(asset.getAssetState()).thenReturn(AssetState.RETIRED);

        RetireExecution execution = new RetireExecution(passport, "outbox-1");
        when(retireExecutor.execute("passport-1", "user-1")).thenReturn(execution);

        RetireResult result = service.retire(actor, command);

        assertEquals("asset-1", result.assetId());
        assertEquals("RETIRED", result.assetState());
        assertEquals("outbox-1", result.outboxEventId());

        verify(retireAccessPolicy).assertRetireAllowed(actor, "passport-1");
    }

    @Test
    void retire_failsWhenAccessDenied() {
        ProductActor actor = new ProductActor("user-1", "brand-tenant", Set.of("OWNER_RETIRE"), false);
        RetireCommand command = new RetireCommand("passport-1");

        ProductDomainException forbidden = new ProductDomainException(
            ProductErrorCode.FORBIDDEN_RETIRE, "Forbidden retire"
        );
        doThrow(forbidden).when(retireAccessPolicy).assertRetireAllowed(actor, "passport-1");

        ProductDomainException ex = assertThrows(ProductDomainException.class, () -> service.retire(actor, command));
        assertEquals(ProductErrorCode.FORBIDDEN_RETIRE, ex.getErrorCode());
    }
}
