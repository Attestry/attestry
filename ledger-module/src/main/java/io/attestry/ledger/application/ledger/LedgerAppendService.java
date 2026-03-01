package io.attestry.ledger.application.ledger;

import io.attestry.ledger.application.ledger.command.AppendLedgerEntryCommand;
import io.attestry.ledger.application.ledger.result.AppendLedgerEntryResult;
import io.attestry.ledger.application.ledger.usecase.LedgerAppendUseCase;
import io.attestry.ledger.application.port.LedgerAppendRepositoryPort;
import io.attestry.ledger.domain.ledger.service.LedgerCanonicalizer;
import io.attestry.ledger.domain.ledger.service.LedgerHashService;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerAppendService implements LedgerAppendUseCase {

    private final LedgerAppendRepositoryPort repository;
    private final LedgerCanonicalizer canonicalizer;
    private final LedgerHashService hashService;
    private final Clock clock;

    public LedgerAppendService(
        LedgerAppendRepositoryPort repository,
        LedgerCanonicalizer canonicalizer,
        LedgerHashService hashService,
        Clock clock
    ) {
        this.repository = repository;
        this.canonicalizer = canonicalizer;
        this.hashService = hashService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AppendLedgerEntryResult append(AppendLedgerEntryCommand command) {
        validate(command);
        Instant occurredAt = command.occurredAt() == null ? Instant.now(clock) : command.occurredAt();
        String payloadCanonical = canonicalizer.canonicalize(command.payload());
        String payloadJson = canonicalizer.serialize(command.payload());
        String dataHash = hashService.dataHash(payloadCanonical);

        LedgerAppendRepositoryPort.AppendOutcome outcome = repository.append(new LedgerAppendRepositoryPort.AppendRequest(
            command.passportId(),
            command.eventCategory(),
            command.eventAction(),
            command.actorRole(),
            command.actorId(),
            occurredAt,
            payloadJson,
            payloadCanonical,
            dataHash,
            normalizeBlank(command.idempotencyKey())
        ));

        return new AppendLedgerEntryResult(
            outcome.entry().ledgerId(),
            outcome.entry().passportId(),
            outcome.entry().seq(),
            outcome.entry().dataHash(),
            outcome.entry().prevHash(),
            outcome.entry().entryHash(),
            outcome.entry().idempotencyKey(),
            outcome.duplicated()
        );
    }

    private void validate(AppendLedgerEntryCommand command) {
        requireText(command.passportId(), "passportId");
        requireText(command.eventCategory(), "eventCategory");
        requireText(command.eventAction(), "eventAction");
        requireText(command.actorRole(), "actorRole");
        requireText(command.actorId(), "actorId");
        if (command.payload() == null) {
            throw new IllegalArgumentException("payload is required");
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private String normalizeBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
