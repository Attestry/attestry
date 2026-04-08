package io.attestry.product.application.command.service;

import io.attestry.product.application.command.ProductVoidUseCase;
import io.attestry.product.application.command.model.VoidCommand;
import io.attestry.product.application.command.result.VoidResult;
import io.attestry.product.application.command.internal.ProductVoidAccessPolicy;
import io.attestry.product.application.command.internal.ProductVoidExecutor;
import io.attestry.product.application.command.internal.VoidExecution;
import io.attestry.product.application.common.ProductActor;
import io.attestry.product.application.port.passport.VoidCommandPort;
import io.attestry.product.domain.passport.model.VoidReason;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductVoidService implements ProductVoidUseCase, VoidCommandPort {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final ProductVoidAccessPolicy voidAccessPolicy;
    private final ProductVoidExecutor voidExecutor;

    @Override
    @Transactional
    public VoidResult voidAsset(ProductActor actor, VoidCommand command) {
        voidAccessPolicy.assertVoidAllowed(actor, command.tenantId(), command.passportId());
        VoidReason reason = parseVoidReason(command.reason());
        VoidExecution execution = voidExecutor.execute(command.passportId(), reason, command.note(), actor.userId());
        return new VoidResult(
            execution.passport().getAsset().getAssetId(),
            execution.passport().getAsset().getAssetState().name(),
            execution.outboxEventId()
        );
    }

    @Override
    @Transactional
    public void voidAsset(String passportId, VoidReason reason, String note) {
        voidExecutor.execute(passportId, reason, note, SYSTEM_ACTOR);
    }

    private VoidReason parseVoidReason(String reason) {
        try {
            return VoidReason.valueOf(reason);
        } catch (IllegalArgumentException e) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "Invalid void reason: " + reason);
        }
    }
}
