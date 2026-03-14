package io.attestry.product.application.command;

import io.attestry.product.application.port.ledger.LedgerOutboxPort;
import io.attestry.product.application.port.passport.PassportPort;
import io.attestry.product.application.command.dto.LedgerActor;
import io.attestry.product.application.command.dto.MintExecution;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.product.domain.event.ProductLedgerEvents;
import io.attestry.product.domain.passport.model.MintProductInput;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.service.QrPublicCodeGenerator;
import io.attestry.product.domain.service.UuidV7Generator;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMintExecutor {

    private final PassportPort passportPort;
    private final LedgerOutboxPort ledgerOutboxPort;
    private final UuidV7Generator uuidV7Generator;
    private final QrPublicCodeGenerator qrPublicCodeGenerator;
    private final Clock clock;

    public MintExecution execute(MintProductInput input, LedgerActor ledgerActor) {
        assertNotDuplicate(input.tenantId(), input.serialNumber());
        ProductPassport passport = createPassport(input);
        ProductPassport savedPassport = passportPort.save(passport);
        OutboxEventEnvelope event = ProductLedgerEvents.minted(
            savedPassport,
            ledgerActor.actorRole(),
            ledgerActor.actorId(),
            savedPassport.getCreatedAt()
        );
        String outboxEventId = ledgerOutboxPort.enqueue(event);
        return new MintExecution(savedPassport, event, outboxEventId);
    }

    private ProductPassport createPassport(MintProductInput input) {
        Instant now = Instant.now(clock);
        return ProductPassport.mint(
            uuidV7Generator.nextId(),
            qrPublicCodeGenerator.nextCode(),
            uuidV7Generator.nextId(),
            input,
            now
        );
    }

    private void assertNotDuplicate(String tenantId, String serialNumber) {
        if (passportPort.existsByTenantAndSerial(tenantId, serialNumber)) {
            throw new ProductDomainException(
                ProductErrorCode.GENESIS_ALREADY_EXISTS,
                "MINTED genesis already exists for tenant/serial"
            );
        }
    }
}
