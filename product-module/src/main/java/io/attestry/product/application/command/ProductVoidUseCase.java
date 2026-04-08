package io.attestry.product.application.command;

import io.attestry.product.application.command.model.VoidCommand;
import io.attestry.product.application.command.result.VoidResult;
import io.attestry.product.application.common.ProductActor;

public interface ProductVoidUseCase {

    VoidResult voidAsset(ProductActor actor, VoidCommand command);
}
