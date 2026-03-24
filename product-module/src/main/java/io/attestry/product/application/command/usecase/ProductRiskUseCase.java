package io.attestry.product.application.command.usecase;

import io.attestry.product.application.command.model.ClearRiskCommand;
import io.attestry.product.application.command.model.FlagLostCommand;
import io.attestry.product.application.command.model.FlagStolenCommand;
import io.attestry.product.application.command.result.RiskResult;
import io.attestry.product.application.common.ProductActor;

public interface ProductRiskUseCase {

    RiskResult flagStolen(ProductActor actor, FlagStolenCommand command);

    RiskResult flagLost(ProductActor actor, FlagLostCommand command);

    RiskResult clearRisk(ProductActor actor, ClearRiskCommand command);
}
