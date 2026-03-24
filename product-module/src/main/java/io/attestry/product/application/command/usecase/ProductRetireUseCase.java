package io.attestry.product.application.command.usecase;

import io.attestry.product.application.command.model.RetireCommand;
import io.attestry.product.application.command.result.RetireResult;
import io.attestry.product.application.common.ProductActor;

public interface ProductRetireUseCase {

    RetireResult retire(ProductActor actor, RetireCommand command);
}
