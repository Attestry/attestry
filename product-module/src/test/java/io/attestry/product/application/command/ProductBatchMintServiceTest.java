package io.attestry.product.application.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.product.application.command.dto.LedgerActor;
import io.attestry.product.application.dto.command.MintProductCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.policy.ProductMintAccessPolicy;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ProductBatchMintServiceTest {

    @Mock ProductMintCsvParser csvParser;
    @Mock ProductMintAccessPolicy mintAccessPolicy;
    @Mock ProductMintExecutor mintExecutor;
    @Mock TransactionTemplate transactionTemplate;

    private ProductBatchMintService service;

    @BeforeEach
    void setUp() {
        service = new ProductBatchMintService(csvParser, mintAccessPolicy, mintExecutor, transactionTemplate);
    }

    @Test
    void batchMint_checksSerialScopedAuthorizationForEachRow() {
        ProductActor actor = new ProductActor("user-1", "brand-tenant", Set.of("BRAND_MINT"), false);
        String tenantId = "brand-tenant";
        List<MintProductCommand> commands = List.of(
            command("SN-001"),
            command("SN-002")
        );
        LedgerActor ledgerActor = new LedgerActor("BRAND", tenantId);

        when(mintAccessPolicy.resolveLedgerActor(actor, tenantId)).thenReturn(ledgerActor);
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        service.batchMint(actor, tenantId, commands);

        verify(mintAccessPolicy).assertMintAllowed(actor, tenantId, "SN-001");
        verify(mintAccessPolicy).assertMintAllowed(actor, tenantId, "SN-002");
    }

    private MintProductCommand command(String serialNumber) {
        return new MintProductCommand(
            "brand-tenant",
            serialNumber,
            "MODEL-1",
            "Model Name",
            Instant.parse("2026-01-01T00:00:00Z"),
            "BATCH-01",
            "FACTORY-A",
            "root-hash"
        );
    }
}
