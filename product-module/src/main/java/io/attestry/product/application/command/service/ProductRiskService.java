package io.attestry.product.application.command.service;

import io.attestry.product.application.command.model.ClearRiskCommand;
import io.attestry.product.application.command.model.FlagLostCommand;
import io.attestry.product.application.command.model.FlagStolenCommand;
import io.attestry.product.application.command.ProductRiskUseCase;
import io.attestry.product.application.command.internal.ProductRiskAccessPolicy;
import io.attestry.product.application.command.internal.ProductRiskExecutor;
import io.attestry.product.application.command.internal.RiskExecution;
import io.attestry.product.application.command.result.RiskResult;
import io.attestry.product.application.common.ProductActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductRiskService implements ProductRiskUseCase {

    private final ProductRiskAccessPolicy riskAccessPolicy;
    private final ProductRiskExecutor riskExecutor;

    @Override
    @Transactional
    public RiskResult flagStolen(ProductActor actor, FlagStolenCommand command) {
        riskAccessPolicy.assertFlagAllowed(actor, command.passportId());
        RiskExecution execution = riskExecutor.flagStolen(command.passportId(), actor.userId(), command.policeReportNo());
        return new RiskResult(
            execution.passport().getAsset().getAssetId(),
            execution.passport().getAsset().getRiskFlag().name(),
            execution.outboxEventId()
        );
    }

    @Override
    @Transactional
    public RiskResult flagLost(ProductActor actor, FlagLostCommand command) {
        riskAccessPolicy.assertFlagAllowed(actor, command.passportId());
        RiskExecution execution = riskExecutor.flagLost(command.passportId(), actor.userId());
        return new RiskResult(
            execution.passport().getAsset().getAssetId(),
            execution.passport().getAsset().getRiskFlag().name(),
            execution.outboxEventId()
        );
    }

    @Override
    @Transactional
    public RiskResult clearRisk(ProductActor actor, ClearRiskCommand command) {
        riskAccessPolicy.assertClearAllowed(actor, command.passportId());
        RiskExecution execution = riskExecutor.clearRisk(command.passportId(), actor.userId());
        return new RiskResult(
            execution.passport().getAsset().getAssetId(),
            execution.passport().getAsset().getRiskFlag().name(),
            execution.outboxEventId()
        );
    }
}
