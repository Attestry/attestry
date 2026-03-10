package io.attestry.product.application.service;

import io.attestry.product.application.dto.command.ClearRiskCommand;
import io.attestry.product.application.dto.command.FlagLostCommand;
import io.attestry.product.application.dto.command.FlagStolenCommand;
import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.result.RiskResult;
import io.attestry.product.application.port.LedgerOutboxPort;
import io.attestry.product.application.port.PassportOwnershipPort;
import io.attestry.product.application.port.PassportPort;
import io.attestry.product.application.port.ProductAuthorizationPort;
import io.attestry.product.application.usecase.ProductRiskUseCase;
import io.attestry.product.domain.passport.model.RiskFlag;
import io.attestry.product.domain.event.LedgerEventEnvelope;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import io.attestry.product.domain.passport.model.ProductPassport;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductRiskService implements ProductRiskUseCase {

    private final PassportPort passportPort;
    private final PassportOwnershipPort ownershipPort;
    private final ProductAuthorizationPort productAuthorizationPort;
    private final LedgerOutboxPort ledgerOutboxPort;
    private final Clock clock;

    public ProductRiskService(
        PassportPort passportPort,
        PassportOwnershipPort ownershipPort,
        ProductAuthorizationPort productAuthorizationPort,
        LedgerOutboxPort ledgerOutboxPort,
        Clock clock
    ) {
        this.passportPort = passportPort;
        this.ownershipPort = ownershipPort;
        this.productAuthorizationPort = productAuthorizationPort;
        this.ledgerOutboxPort = ledgerOutboxPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RiskResult flagStolen(ProductActor actor, FlagStolenCommand command) {
        productAuthorizationPort.assertOwnerRiskFlagAllowed(actor);
        assertOwnership(actor, command.passportId());

        Instant now = Instant.now(clock);
        ProductPassport passport = findPassport(command.passportId());
        passport.flagStolen(actor.userId(), command.policeReportNo(), now);
        passportPort.save(passport);

        LedgerEventEnvelope event = LedgerEventEnvelope.riskFlagged(passport, actor.userId(), now);
        String outboxEventId = enqueueSafe(event);

        return new RiskResult(passport.getAsset().getAssetId(), passport.getAsset().getRiskFlag().name(), outboxEventId);
    }

    @Override
    @Transactional
    public RiskResult flagLost(ProductActor actor, FlagLostCommand command) {
        productAuthorizationPort.assertOwnerRiskFlagAllowed(actor);
        assertOwnership(actor, command.passportId());

        Instant now = Instant.now(clock);
        ProductPassport passport = findPassport(command.passportId());
        passport.flagLost(actor.userId(), now);
        passportPort.save(passport);

        LedgerEventEnvelope event = LedgerEventEnvelope.riskFlagged(passport, actor.userId(), now);
        String outboxEventId = enqueueSafe(event);

        return new RiskResult(passport.getAsset().getAssetId(), passport.getAsset().getRiskFlag().name(), outboxEventId);
    }

    @Override
    @Transactional
    public RiskResult clearRisk(ProductActor actor, ClearRiskCommand command) {
        productAuthorizationPort.assertOwnerRiskClearAllowed(actor);
        assertOwnership(actor, command.passportId());

        Instant now = Instant.now(clock);
        ProductPassport passport = findPassport(command.passportId());

        if (passport.getAsset().getRiskFlag() == RiskFlag.NONE) {
            throw new ProductDomainException(ProductErrorCode.RISK_FLAG_NOT_SET,
                "No risk flag to clear for asset: " + passport.getAsset().getAssetId());
        }

        passport.clearRisk();
        passportPort.save(passport);

        LedgerEventEnvelope event = LedgerEventEnvelope.riskCleared(passport, actor.userId(), now);
        String outboxEventId = enqueueSafe(event);

        return new RiskResult(passport.getAsset().getAssetId(), passport.getAsset().getRiskFlag().name(), outboxEventId);
    }

    private ProductPassport findPassport(String passportId) {
        return passportPort.findById(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found: " + passportId));
    }

    private void assertOwnership(ProductActor actor, String passportId) {
        PassportOwnership ownership = ownershipPort.findByPassportId(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.NOT_ASSET_OWNER,
                "No ownership record for passport: " + passportId));
        if (!actor.userId().equals(ownership.getOwnerId())) {
            throw new ProductDomainException(ProductErrorCode.NOT_ASSET_OWNER,
                "Actor is not the owner of passport: " + passportId);
        }
    }

    private String enqueueSafe(LedgerEventEnvelope event) {
        try {
            return ledgerOutboxPort.enqueue(event);
        } catch (RuntimeException ex) {
            throw new ProductDomainException(ProductErrorCode.OUTBOX_ENQUEUE_FAILED, "Failed to enqueue ledger outbox event");
        }
    }
}
