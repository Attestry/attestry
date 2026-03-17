package io.attestry.product.application.command;

import io.attestry.product.application.command.dto.RetireExecution;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.command.RetireCommand;
import io.attestry.product.application.dto.result.RetireResult;
import io.attestry.product.application.policy.ProductRetireAccessPolicy;
import io.attestry.product.application.usecase.ProductRetireUseCase;
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
