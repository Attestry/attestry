package io.attestry.product.application.command.support;

import io.attestry.product.application.port.ledger.LedgerOutboxPort;
import io.attestry.product.application.port.passport.PassportPort;
import io.attestry.product.application.command.support.VoidExecution;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.product.application.event.ProductLedgerEvents;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.passport.model.VoidReason;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductVoidExecutor {

    private final PassportPort passportPort;
    private final LedgerOutboxPort ledgerOutboxPort;
    private final Clock clock;

    public VoidExecution execute(String passportId, VoidReason reason, String note, String actorId) {
        Instant now = Instant.now(clock);
        ProductPassport passport = findPassport(passportId);
        passport.voidAsset(reason, note, now);
        passportPort.save(passport);
        OutboxEventEnvelope event = ProductLedgerEvents.voided(passport, actorId, now);
        return new VoidExecution(passport, enqueueSafe(event));
    }

    private ProductPassport findPassport(String passportId) {
        return passportPort.findById(passportId)
            .orElseThrow(() -> new ProductDomainException(
                ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found: " + passportId
            ));
    }

    private String enqueueSafe(OutboxEventEnvelope event) {
        try {
            return ledgerOutboxPort.enqueue(event);
        } catch (RuntimeException ex) {
            throw new ProductDomainException(ProductErrorCode.OUTBOX_ENQUEUE_FAILED, "Failed to enqueue ledger outbox event");
        }
    }
}
