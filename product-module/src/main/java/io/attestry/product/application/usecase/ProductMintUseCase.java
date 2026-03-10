package io.attestry.product.application.usecase;

import io.attestry.product.application.dto.command.MintProductCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.result.BatchMintResult;
import io.attestry.product.application.dto.result.MintedProductResult;
import java.io.InputStream;
import java.util.List;

public interface ProductMintUseCase {

    MintedProductResult mint(ProductActor actor, MintProductCommand command);

    BatchMintResult batchMint(ProductActor actor, String tenantId, List<MintProductCommand> commands);

    BatchMintResult batchMintFromCsv(ProductActor actor, String tenantId, InputStream csvStream);
}
