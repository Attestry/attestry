package io.attestry.adapters.workflow;

import io.attestry.product.application.dto.command.MintProductCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.result.MintedProductResult;
import io.attestry.product.application.usecase.ProductMintUseCase;
import io.attestry.workflow.application.port.claim.PurchaseClaimProductMintPort;
import org.springframework.stereotype.Component;

@Component
public class PurchaseClaimProductMintAdapter implements PurchaseClaimProductMintPort {

    private final ProductMintUseCase productMintUseCase;

    public PurchaseClaimProductMintAdapter(ProductMintUseCase productMintUseCase) {
        this.productMintUseCase = productMintUseCase;
    }

    @Override
    public MintResult mint(MintRequest request) {
        MintedProductResult result = productMintUseCase.mint(
            new ProductActor(
                request.actorUserId(),
                request.actorTenantId(),
                request.actorScopes(),
                request.platformAdmin()
            ),
            new MintProductCommand(
                request.tenantId(),
                request.serialNumber(),
                null,
                request.modelName(),
                request.manufacturedAt(),
                request.productionBatch(),
                request.factoryCode(),
                null
            )
        );
        return new MintResult(
            result.assetId(),
            result.passportId(),
            result.qrPublicCode(),
            result.outboxEventId(),
            result.ledgerEventCategory(),
            result.ledgerEventAction()
        );
    }
}
