package io.attestry.product.application.usecase;

import io.attestry.product.application.dto.command.ClearRiskCommand;
import io.attestry.product.application.dto.command.FlagLostCommand;
import io.attestry.product.application.dto.command.FlagStolenCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.result.RiskResult;

public interface ProductRiskUseCase {

    RiskResult flagStolen(ProductActor actor, FlagStolenCommand command);

    RiskResult flagLost(ProductActor actor, FlagLostCommand command);

    RiskResult clearRisk(ProductActor actor, ClearRiskCommand command);
}
