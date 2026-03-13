package io.attestry.product.application.usecase;

import io.attestry.product.application.dto.command.VoidCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.result.VoidResult;

public interface ProductVoidUseCase {

    VoidResult voidAsset(ProductActor actor, VoidCommand command);
}
