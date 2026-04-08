package io.attestry.product.application.command.internal;

import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.product.application.port.ledger.LedgerOutboxPort;
import io.attestry.product.application.port.passport.PassportPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.application.event.ProductLedgerEvents;
import io.attestry.product.domain.passport.model.ProductPassport;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductRetireExecutor {

    private final PassportPort passportPort;
    private final LedgerOutboxPort ledgerOutboxPort;
    private final Clock clock;

    public RetireExecution execute(String passportId, String actorId) {
        Instant now = Instant.now(clock);
        ProductPassport passport = passportPort.findById(passportId)
            .orElseThrow(() -> new ProductDomainException(
                ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found: " + passportId
            ));

        passport.retire(now);
        passportPort.save(passport);

        OutboxEventEnvelope event = ProductLedgerEvents.retired(passport, actorId, now);
        try {
            return new RetireExecution(passport, ledgerOutboxPort.enqueue(event));
        } catch (RuntimeException ex) {
            throw new ProductDomainException(ProductErrorCode.OUTBOX_ENQUEUE_FAILED, "Failed to enqueue ledger outbox event");
        }
    }
}
