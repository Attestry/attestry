package io.attestry.product.application.command.usecase;

import io.attestry.product.application.command.model.MintProductCommand;
import io.attestry.product.application.command.result.BatchMintResult;
import io.attestry.product.application.command.result.MintedProductResult;
import io.attestry.product.application.common.ProductActor;
import java.io.InputStream;

public interface ProductMintUseCase {

    MintedProductResult mint(ProductActor actor, MintProductCommand command);


    BatchMintResult batchMintFromCsv(ProductActor actor, String tenantId, InputStream csvStream);
}
