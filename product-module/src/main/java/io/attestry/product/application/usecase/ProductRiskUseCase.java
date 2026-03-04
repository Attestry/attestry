package io.attestry.product.application.usecase;

import io.attestry.userauth.application.dto.command.ActorContext;

public interface ProductRiskUseCase {

    RiskResult flagStolen(ActorContext actor, FlagStolenCommand command);

    RiskResult flagLost(ActorContext actor, FlagLostCommand command);

    RiskResult clearRisk(ActorContext actor, ClearRiskCommand command);

    record FlagStolenCommand(String passportId, String policeReportNo) {
    }

    record FlagLostCommand(String passportId) {
    }

    record ClearRiskCommand(String passportId) {
    }

    record RiskResult(String assetId, String riskFlag, String outboxEventId) {
    }
}
