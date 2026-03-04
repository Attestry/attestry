package io.attestry.product.interfaces.http;

import io.attestry.product.application.usecase.ProductRiskUseCase;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.security.CurrentActor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductRiskHttp {

    private final ProductRiskUseCase riskUseCase;

    public ProductRiskHttp(ProductRiskUseCase riskUseCase) {
        this.riskUseCase = riskUseCase;
    }

    @PostMapping("/passports/{passportId}/risk/stolen")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_RISK_FLAG')")
    public RiskResponse flagStolen(
        @CurrentActor ActorContext actor,
        @PathVariable("passportId") String passportId,
        @RequestBody FlagStolenRequest request
    ) {
        ProductRiskUseCase.RiskResult result = riskUseCase.flagStolen(
            actor,
            new ProductRiskUseCase.FlagStolenCommand(passportId, request.policeReportNo())
        );
        return new RiskResponse(result.assetId(), result.riskFlag(), result.outboxEventId());
    }

    @PostMapping("/passports/{passportId}/risk/lost")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_RISK_FLAG')")
    public RiskResponse flagLost(
        @CurrentActor ActorContext actor,
        @PathVariable("passportId") String passportId
    ) {
        ProductRiskUseCase.RiskResult result = riskUseCase.flagLost(
            actor,
            new ProductRiskUseCase.FlagLostCommand(passportId)
        );
        return new RiskResponse(result.assetId(), result.riskFlag(), result.outboxEventId());
    }


    @PreAuthorize("hasAuthority('SCOPE_OWNER_RISK_CLEAR')")
    @DeleteMapping("/passports/{passportId}/risk")
    public RiskResponse clearRisk(
        @CurrentActor ActorContext actor,
        @PathVariable("passportId") String passportId
    ) {
        ProductRiskUseCase.RiskResult result = riskUseCase.clearRisk(
            actor,
            new ProductRiskUseCase.ClearRiskCommand(passportId)
        );
        return new RiskResponse(result.assetId(), result.riskFlag(), result.outboxEventId());
    }

    public record FlagStolenRequest(String policeReportNo) {
    }

    public record RiskResponse(String assetId, String riskFlag, String outboxEventId) {
    }
}
