package io.attestry.product.application.service;

import io.attestry.product.application.port.BrandAccessValidationPort;
import io.attestry.product.application.port.LedgerOutboxPort;
import io.attestry.product.application.port.VoidCommandPort;
import io.attestry.product.application.usecase.ProductVoidUseCase;
import io.attestry.product.domain.passport.model.VoidReason;
import io.attestry.product.domain.event.LedgerEventEnvelope;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
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
public class ProductVoidService implements ProductVoidUseCase, VoidCommandPort {

    private final PassportRepository passportRepository;
    private final BrandAccessValidationPort brandAccessValidationPort;
    private final LedgerOutboxPort ledgerOutboxPort;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final Clock clock;

    public ProductVoidService(
        PassportRepository passportRepository,
        BrandAccessValidationPort brandAccessValidationPort,
        LedgerOutboxPort ledgerOutboxPort,
        EvaluateAuthorizationUseCase evaluateAuthorizationUseCase,
        Clock clock
    ) {
        this.passportRepository = passportRepository;
        this.brandAccessValidationPort = brandAccessValidationPort;
        this.ledgerOutboxPort = ledgerOutboxPort;
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional
    public VoidResult voidAsset(ActorContext actor, VoidCommand command) {
        brandAccessValidationPort.assertActiveBrandMembership(actor.userId(), command.tenantId(), command.groupId());
        assertBrandVoidScope(actor, command.tenantId(), command.passportId());

        VoidReason reason = parseVoidReason(command.reason());
        Instant now = Instant.now(clock);

        ProductPassport passport = findPassport(command.passportId());
        passport.voidAsset(reason, command.note(), now);
        passportRepository.save(passport);

        LedgerEventEnvelope event = LedgerEventEnvelope.voided(passport, actor.userId(), now);
        String outboxEventId = enqueueSafe(event);

        return new VoidResult(passport.getAsset().getAssetId(), passport.getAsset().getAssetState().name(), outboxEventId);
    }

    @Override
    @Transactional
    public void voidAsset(String passportId, VoidReason reason, String note) {
        Instant now = Instant.now(clock);

        ProductPassport passport = findPassport(passportId);
        passport.voidAsset(reason, note, now);
        passportRepository.save(passport);

        LedgerEventEnvelope event = LedgerEventEnvelope.voided(passport, "SYSTEM", now);
        enqueueSafe(event);
    }

    private ProductPassport findPassport(String passportId) {
        return passportRepository.findById(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found: " + passportId));
    }

    private void assertBrandVoidScope(ActorContext actor, String tenantId, String passportId) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            actor,
            new AuthzEvaluateCommand(
                tenantId,
                PermissionCodes.BRAND_VOID,
                "void:" + passportId,
                PolicyDecisionMode.LIVE_RECHECK
            )
        );
        if (!decision.allowed()) {
            throw new ProductDomainException(ProductErrorCode.FORBIDDEN_VOID, "BRAND_VOID scope is required");
        }
    }

    private VoidReason parseVoidReason(String reason) {
        try {
            return VoidReason.valueOf(reason);
        } catch (IllegalArgumentException e) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "Invalid void reason: " + reason);
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
