package io.attestry.product.application.service;

import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.command.ProductTenantType;
import io.attestry.product.application.dto.command.VoidCommand;
import io.attestry.product.application.dto.result.VoidResult;
import io.attestry.product.application.port.LedgerOutboxPort;
import io.attestry.product.application.port.PassportPort;
import io.attestry.product.application.port.ProductAuthorizationPort;
import io.attestry.product.application.port.TenantContextAccessPort;
import io.attestry.product.application.port.VoidCommandPort;
import io.attestry.product.application.usecase.ProductVoidUseCase;
import io.attestry.product.domain.passport.model.VoidReason;
import io.attestry.product.domain.event.LedgerEventEnvelope;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.passport.model.ProductPassport;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductVoidService implements ProductVoidUseCase, VoidCommandPort {

    private final PassportPort passportPort;
    private final TenantContextAccessPort tenantContextAccessPort;
    private final ProductAuthorizationPort productAuthorizationPort;
    private final LedgerOutboxPort ledgerOutboxPort;
    private final Clock clock;

    public ProductVoidService(
        PassportPort passportPort,
        TenantContextAccessPort tenantContextAccessPort,
        LedgerOutboxPort ledgerOutboxPort,
        ProductAuthorizationPort productAuthorizationPort,
        Clock clock
    ) {
        this.passportPort = passportPort;
        this.tenantContextAccessPort = tenantContextAccessPort;
        this.productAuthorizationPort = productAuthorizationPort;
        this.ledgerOutboxPort = ledgerOutboxPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public VoidResult voidAsset(ProductActor actor, VoidCommand command) {
        tenantContextAccessPort.assertActiveTenantMembership(actor.userId(), command.tenantId(), ProductTenantType.BRAND);
        productAuthorizationPort.assertBrandVoidAllowed(actor, command.tenantId(), command.passportId());

        VoidReason reason = parseVoidReason(command.reason());
        Instant now = Instant.now(clock);

        ProductPassport passport = findPassport(command.passportId());
        passport.voidAsset(reason, command.note(), now);
        passportPort.save(passport);

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
        passportPort.save(passport);

        LedgerEventEnvelope event = LedgerEventEnvelope.voided(passport, "SYSTEM", now);
        enqueueSafe(event);
    }

    private ProductPassport findPassport(String passportId) {
        return passportPort.findById(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found: " + passportId));
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
