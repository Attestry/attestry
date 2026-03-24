package io.attestry.product.application.command.service;

import io.attestry.product.application.command.model.MintProductCommand;
import io.attestry.product.application.common.ProductActor;
import io.attestry.product.application.command.result.BatchMintError;
import io.attestry.product.application.command.result.BatchMintResult;
import io.attestry.product.application.command.support.LedgerActor;
import io.attestry.product.application.command.support.ProductMintCsvParser;
import io.attestry.product.application.command.support.ProductMintExecutor;
import io.attestry.product.application.policy.ProductMintAccessPolicy;
import io.attestry.product.domain.passport.model.MintProductInput;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class ProductBatchMintService {

    private final ProductMintCsvParser csvParser;
    private final ProductMintAccessPolicy mintAccessPolicy;
    private final ProductMintExecutor mintExecutor;
    private final TransactionTemplate transactionTemplate;

    public BatchMintResult batchMintFromCsv(ProductActor actor, String tenantId, InputStream csvStream) {
        List<MintProductCommand> commands = csvParser.parse(tenantId, csvStream);
        return batchMint(actor, tenantId, commands);
    }

    public BatchMintResult batchMint(ProductActor actor, String tenantId, List<MintProductCommand> commands) {
        LedgerActor ledgerActor = mintAccessPolicy.resolveLedgerActor(actor, tenantId);

        List<BatchMintError> errors = new ArrayList<>();
        int minted = 0;

        for (int i = 0; i < commands.size(); i++) {
            MintProductCommand command = commands.get(i);
            int row = i + 1;
            try {
                mintAccessPolicy.assertMintAllowed(actor, tenantId, command.serialNumber());
                transactionTemplate.executeWithoutResult(status -> mintSingle(tenantId, command, ledgerActor));
                minted++;
            } catch (Exception ex) {
                errors.add(new BatchMintError(row, command.serialNumber(), ex.getMessage()));
            }
        }

        return new BatchMintResult(commands.size(), minted, errors.size(), errors);
    }

    private void mintSingle(String tenantId, MintProductCommand command, LedgerActor ledgerActor) {
        mintExecutor.execute(toInput(tenantId, command), ledgerActor);
    }

    private MintProductInput toInput(String tenantId, MintProductCommand command) {
        return MintProductInput.of(
            tenantId,
            command.serialNumber(),
            command.modelId(),
            command.modelName(),
            command.manufacturedAt(),
            command.productionBatch(),
            command.factoryCode(),
            command.componentRootHash()
        );
    }
}
