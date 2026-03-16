package io.attestry.product.application.usecase;

import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.command.RetireCommand;
import io.attestry.product.application.dto.result.RetireResult;

public interface ProductRetireUseCase {

    RetireResult retire(ProductActor actor, RetireCommand command);
}
