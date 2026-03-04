package io.attestry.product.application.service;

import io.attestry.product.application.port.LedgerOutboxPort;
import io.attestry.product.application.usecase.ProductRiskUseCase;
import io.attestry.product.domain.passport.model.RiskFlag;
import io.attestry.product.domain.event.LedgerEventEnvelope;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import io.attestry.product.domain.ownership.repository.PassportOwnershipRepository;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.passport.repository.PassportRepository;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductRiskService implements ProductRiskUseCase {

    private final PassportRepository passportRepository;
    private final PassportOwnershipRepository ownershipRepository;
    private final LedgerOutboxPort ledgerOutboxPort;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final Clock clock;

    public ProductRiskService(
        PassportRepository passportRepository,
        PassportOwnershipRepository ownershipRepository,
        LedgerOutboxPort ledgerOutboxPort,
        EvaluateAuthorizationUseCase evaluateAuthorizationUseCase,
        Clock clock
    ) {
        this.passportRepository = passportRepository;
        this.ownershipRepository = ownershipRepository;
        this.ledgerOutboxPort = ledgerOutboxPort;
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RiskResult flagStolen(ActorContext actor, FlagStolenCommand command) {
        assertRiskFlagScope(actor);
        assertOwnership(actor, command.passportId());

        Instant now = Instant.now(clock);
        ProductPassport passport = findPassport(command.passportId());
        passport.flagStolen(actor.userId(), command.policeReportNo(), now);
        passportRepository.save(passport);

        LedgerEventEnvelope event = LedgerEventEnvelope.riskFlagged(passport, actor.userId(), now);
        String outboxEventId = enqueueSafe(event);

        return new RiskResult(passport.getAsset().getAssetId(), passport.getAsset().getRiskFlag().name(), outboxEventId);
    }

    @Override
    @Transactional
    public RiskResult flagLost(ActorContext actor, FlagLostCommand command) {
        assertRiskFlagScope(actor);
        assertOwnership(actor, command.passportId());

        Instant now = Instant.now(clock);
        ProductPassport passport = findPassport(command.passportId());
        passport.flagLost(actor.userId(), now);
        passportRepository.save(passport);

        LedgerEventEnvelope event = LedgerEventEnvelope.riskFlagged(passport, actor.userId(), now);
        String outboxEventId = enqueueSafe(event);

        return new RiskResult(passport.getAsset().getAssetId(), passport.getAsset().getRiskFlag().name(), outboxEventId);
    }

    @Override
    @Transactional
    public RiskResult clearRisk(ActorContext actor, ClearRiskCommand command) {
        assertRiskClearScope(actor);
        assertOwnership(actor, command.passportId());

        Instant now = Instant.now(clock);
        ProductPassport passport = findPassport(command.passportId());

        if (passport.getAsset().getRiskFlag() == RiskFlag.NONE) {
            throw new ProductDomainException(ProductErrorCode.RISK_FLAG_NOT_SET,
                "No risk flag to clear for asset: " + passport.getAsset().getAssetId());
        }

        passport.clearRisk();
        passportRepository.save(passport);

        LedgerEventEnvelope event = LedgerEventEnvelope.riskCleared(passport, actor.userId(), now);
        String outboxEventId = enqueueSafe(event);

        return new RiskResult(passport.getAsset().getAssetId(), passport.getAsset().getRiskFlag().name(), outboxEventId);
    }

    private ProductPassport findPassport(String passportId) {
        return passportRepository.findById(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found: " + passportId));
    }

    private void assertOwnership(ActorContext actor, String passportId) {
        PassportOwnership ownership = ownershipRepository.findByPassportId(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.NOT_ASSET_OWNER,
                "No ownership record for passport: " + passportId));
        if (!actor.userId().equals(ownership.getOwnerId())) {
            throw new ProductDomainException(ProductErrorCode.NOT_ASSET_OWNER,
                "Actor is not the owner of passport: " + passportId);
        }
    }

    private void assertRiskFlagScope(ActorContext actor) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            actor,
            new AuthzEvaluateCommand(actor.tenantId(), PermissionCodes.OWNER_RISK_FLAG, null, PolicyDecisionMode.TOKEN_SNAPSHOT)
        );
        if (!decision.allowed()) {
            throw new ProductDomainException(ProductErrorCode.FORBIDDEN_RISK_FLAG, "OWNER_RISK_FLAG scope is required");
        }
    }

    private void assertRiskClearScope(ActorContext actor) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            actor,
            new AuthzEvaluateCommand(actor.tenantId(), PermissionCodes.OWNER_RISK_CLEAR, null, PolicyDecisionMode.TOKEN_SNAPSHOT)
        );
        if (!decision.allowed()) {
            throw new ProductDomainException(ProductErrorCode.FORBIDDEN_RISK_FLAG, "OWNER_RISK_CLEAR scope is required");
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
