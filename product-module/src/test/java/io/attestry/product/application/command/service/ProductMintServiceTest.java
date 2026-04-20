package io.attestry.product.application.command.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.product.application.command.internal.LedgerActor;
import io.attestry.product.application.command.internal.MintExecution;
import io.attestry.product.application.command.internal.ProductMintAccessPolicy;
import io.attestry.product.application.command.internal.ProductMintExecutor;
import io.attestry.product.application.command.model.MintProductCommand;
import io.attestry.product.application.command.result.BatchMintResult;
import io.attestry.product.application.command.result.MintedProductResult;
import io.attestry.product.application.common.ProductActor;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.passport.model.AssetState;
import io.attestry.product.domain.passport.model.ProductAsset;
import io.attestry.product.domain.passport.model.ProductPassport;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductMintServiceTest {

    @Mock ProductMintAccessPolicy mintAccessPolicy;
    @Mock ProductMintExecutor mintExecutor;
    @Mock ProductBatchMintService batchMintService;

    private ProductMintService service;

    @BeforeEach
    void setUp() {
        service = new ProductMintService(mintAccessPolicy, mintExecutor, batchMintService);
    }

    @Test
    void mint_success() {
        ProductActor actor = new ProductActor("user-1", "brand-tenant", Set.of("BRAND_MINT"), false);
        MintProductCommand command = new MintProductCommand(
            "brand-tenant", "SN-001", "MODEL-1", "Model Name",
            Instant.parse("2026-01-01T00:00:00Z"), "BATCH-01", "FACTORY-A", null
        );
        LedgerActor ledgerActor = new LedgerActor("BRAND", "brand-tenant");

        ProductPassport passport = mock(ProductPassport.class);
        ProductAsset asset = mock(ProductAsset.class);
        when(passport.getAsset()).thenReturn(asset);
        when(asset.getAssetId()).thenReturn("asset-1");
        when(passport.getPassportId()).thenReturn("passport-1");
        when(passport.getQrPublicCode()).thenReturn("QR-CODE-1");

        OutboxEventEnvelope event = new OutboxEventEnvelope(
            "ProductPassport", "passport-1", "PRODUCT", "MINTED",
            "BRAND", "brand-tenant", Instant.now(), Map.of(), "idem-key-1"
        );
        MintExecution execution = new MintExecution(passport, event, "outbox-1");

        when(mintAccessPolicy.resolveLedgerActor(actor, "brand-tenant")).thenReturn(ledgerActor);
        when(mintExecutor.execute(any(), eq(ledgerActor))).thenReturn(execution);

        MintedProductResult result = service.mint(actor, command);

        assertEquals("asset-1", result.assetId());
        assertEquals("passport-1", result.passportId());
        assertEquals("QR-CODE-1", result.qrPublicCode());
        assertEquals("outbox-1", result.outboxEventId());
        assertEquals("PRODUCT", result.ledgerEventCategory());
        assertEquals("MINTED", result.ledgerEventAction());

        verify(mintAccessPolicy).assertMintAllowed(actor, "brand-tenant", "SN-001");
    }

    @Test
    void mint_failsWhenAccessDenied() {
        ProductActor actor = new ProductActor("user-1", "brand-tenant", Set.of("BRAND_MINT"), false);
        MintProductCommand command = new MintProductCommand(
            "brand-tenant", "SN-001", "MODEL-1", "Model Name",
            Instant.parse("2026-01-01T00:00:00Z"), "BATCH-01", "FACTORY-A", null
        );

        ProductDomainException forbidden = new ProductDomainException(
            ProductErrorCode.FORBIDDEN_MINT, "Forbidden mint"
        );
        org.mockito.Mockito.doThrow(forbidden)
            .when(mintAccessPolicy).assertMintAllowed(actor, "brand-tenant", "SN-001");

        ProductDomainException ex = assertThrows(ProductDomainException.class, () -> service.mint(actor, command));
        assertEquals(ProductErrorCode.FORBIDDEN_MINT, ex.getErrorCode());
    }

    @Test
    void batchMintFromCsv_delegatesToBatchService() {
        ProductActor actor = new ProductActor("user-1", "brand-tenant", Set.of("BRAND_MINT"), false);
        String tenantId = "brand-tenant";
        InputStream csvStream = new ByteArrayInputStream("csv-data".getBytes(StandardCharsets.UTF_8));
        BatchMintResult expected = new BatchMintResult(2, 2, 0, List.of());

        when(batchMintService.batchMintFromCsv(actor, tenantId, csvStream)).thenReturn(expected);

        BatchMintResult result = service.batchMintFromCsv(actor, tenantId, csvStream);

        assertEquals(expected, result);
        verify(batchMintService).batchMintFromCsv(actor, tenantId, csvStream);
    }
}
