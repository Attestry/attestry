package io.attestry.product.application.command;

import io.attestry.product.application.port.ledger.LedgerOutboxPort;
import io.attestry.product.application.port.passport.PassportPort;
import io.attestry.product.application.command.dto.RiskExecution;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.event.LedgerEventEnvelope;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.passport.model.RiskFlag;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductRiskExecutor {

    private final PassportPort passportPort;
    private final LedgerOutboxPort ledgerOutboxPort;
    private final Clock clock;

    public RiskExecution flagStolen(String passportId, String actorId, String policeReportNo) {
        Instant now = Instant.now(clock);
        ProductPassport passport = findPassport(passportId);
        passport.flagStolen(actorId, policeReportNo, now);
        passportPort.save(passport);
        return new RiskExecution(passport, enqueueSafe(LedgerEventEnvelope.riskFlagged(passport, actorId, now)));
    }

    public RiskExecution flagLost(String passportId, String actorId) {
        Instant now = Instant.now(clock);
        ProductPassport passport = findPassport(passportId);
        passport.flagLost(actorId, now);
        passportPort.save(passport);
        return new RiskExecution(passport, enqueueSafe(LedgerEventEnvelope.riskFlagged(passport, actorId, now)));
    }

    public RiskExecution clearRisk(String passportId, String actorId) {
        Instant now = Instant.now(clock);
        ProductPassport passport = findPassport(passportId);
        RiskFlag currentRiskFlag = passport.getAsset().getRiskFlag();
        if (currentRiskFlag == RiskFlag.NONE) {
            throw new ProductDomainException(
                ProductErrorCode.RISK_FLAG_NOT_SET,
                "No risk flag to clear for asset: " + passport.getAsset().getAssetId()
            );
        }
        passport.clearRisk();
        passportPort.save(passport);
        return new RiskExecution(passport, enqueueSafe(LedgerEventEnvelope.riskCleared(passport, actorId, now, currentRiskFlag)));
    }

    private ProductPassport findPassport(String passportId) {
        return passportPort.findById(passportId)
            .orElseThrow(() -> new ProductDomainException(
                ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found: " + passportId
            ));
    }

    private String enqueueSafe(LedgerEventEnvelope event) {
        try {
            return ledgerOutboxPort.enqueue(event);
        } catch (RuntimeException ex) {
            throw new ProductDomainException(ProductErrorCode.OUTBOX_ENQUEUE_FAILED, "Failed to enqueue ledger outbox event");
        }
    }
}
