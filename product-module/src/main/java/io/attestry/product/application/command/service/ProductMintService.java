package io.attestry.product.application.command.service;

import io.attestry.product.application.command.model.MintProductCommand;
import io.attestry.product.application.common.ProductActor;
import io.attestry.product.application.command.result.BatchMintResult;
import io.attestry.product.application.command.result.MintedProductResult;
import io.attestry.product.application.command.support.MintExecution;
import io.attestry.product.application.command.support.ProductMintExecutor;
import io.attestry.product.application.policy.ProductMintAccessPolicy;
import io.attestry.product.application.command.usecase.ProductMintUseCase;
import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.product.domain.passport.model.MintProductInput;
import io.attestry.product.domain.passport.model.ProductPassport;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ProductMintService implements ProductMintUseCase {

    private final ProductMintAccessPolicy mintAccessPolicy;
    private final ProductMintExecutor mintExecutor;
    private final ProductBatchMintService batchMintService;

    @Override
    @Transactional
    public MintedProductResult mint(ProductActor actor, MintProductCommand command) {
        MintProductInput input = toInput(command.tenantId(), command);
        mintAccessPolicy.assertMintAllowed(actor, input.tenantId(), input.serialNumber());
        MintExecution execution = mintExecutor.execute(
            input,
            mintAccessPolicy.resolveLedgerActor(actor, input.tenantId())
        );
        return toMintedProductResult(execution);
    }

    @Override
    public BatchMintResult batchMintFromCsv(ProductActor actor, String tenantId, InputStream csvStream) {
        return batchMintService.batchMintFromCsv(actor, tenantId, csvStream);
    }

    private MintedProductResult toMintedProductResult(MintExecution execution) {
        ProductPassport passport = execution.passport();
        OutboxEventEnvelope event = execution.event();
        return new MintedProductResult(
            passport.getAsset().getAssetId(),
            passport.getPassportId(),
            passport.getQrPublicCode(),
            execution.outboxEventId(),
            event.eventCategory(),
            event.eventAction()
        );
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
