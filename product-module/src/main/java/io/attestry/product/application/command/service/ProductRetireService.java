package io.attestry.product.application.command.service;

import io.attestry.product.application.command.ProductRetireUseCase;
import io.attestry.product.application.command.internal.ProductRetireAccessPolicy;
import io.attestry.product.application.command.internal.ProductRetireExecutor;
import io.attestry.product.application.command.internal.RetireExecution;
import io.attestry.product.application.command.model.RetireCommand;
import io.attestry.product.application.command.result.RetireResult;
import io.attestry.product.application.common.ProductActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductRetireService implements ProductRetireUseCase {

    private final ProductRetireAccessPolicy retireAccessPolicy;
    private final ProductRetireExecutor retireExecutor;

    @Override
    @Transactional
    public RetireResult retire(ProductActor actor, RetireCommand command) {
        retireAccessPolicy.assertRetireAllowed(actor, command.passportId());
        RetireExecution execution = retireExecutor.execute(command.passportId(), actor.userId());
        return new RetireResult(
            execution.passport().getAsset().getAssetId(),
            execution.passport().getAsset().getAssetState().name(),
            execution.outboxEventId()
        );
    }
}
