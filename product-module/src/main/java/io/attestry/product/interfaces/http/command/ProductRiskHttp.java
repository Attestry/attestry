package io.attestry.product.interfaces.http.command;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.commonlib.web.CurrentActor;
import io.attestry.product.application.dto.command.ClearRiskCommand;
import io.attestry.product.application.dto.command.FlagLostCommand;
import io.attestry.product.application.dto.command.FlagStolenCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.usecase.ProductRiskUseCase;
import io.attestry.product.interfaces.http.command.dto.request.FlagStolenRequest;
import io.attestry.product.interfaces.http.command.dto.response.RiskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductRiskHttp {

    private final ProductRiskUseCase riskUseCase;


    @PostMapping("/passports/{passportId}/risk/stolen")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_RISK_FLAG')")
    public ApiResponse<RiskResponse> flagStolen(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId,
        @RequestBody(required = false) FlagStolenRequest request
    ) {
        return ApiResponse.success(RiskResponse.from(riskUseCase.flagStolen(
            actor,
            new FlagStolenCommand(passportId, request != null ? request.policeReportNo() : null)
        )));
    }

    @PostMapping("/passports/{passportId}/risk/lost")
    @PreAuthorize("hasAuthority('SCOPE_OWNER_RISK_FLAG')")
    public ApiResponse<RiskResponse> flagLost(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId
    ) {
        return ApiResponse.success(RiskResponse.from(riskUseCase.flagLost(
            actor,
            new FlagLostCommand(passportId)
        )));
    }

    @PreAuthorize("hasAuthority('SCOPE_OWNER_RISK_CLEAR')")
    @DeleteMapping("/passports/{passportId}/risk")
    public ApiResponse<RiskResponse> clearRisk(
        @CurrentActor ProductActor actor,
        @PathVariable("passportId") String passportId
    ) {
        return ApiResponse.success(RiskResponse.from(riskUseCase.clearRisk(
            actor,
            new ClearRiskCommand(passportId)
        )));
    }
}
