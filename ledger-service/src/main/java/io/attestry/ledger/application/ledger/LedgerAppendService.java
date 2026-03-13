package io.attestry.ledger.application.ledger;

import io.attestry.ledger.application.ledger.command.AppendLedgerEntryCommand;
import io.attestry.ledger.application.ledger.result.AppendLedgerEntryResult;
import io.attestry.ledger.application.usecase.LedgerAppendUseCase;
import io.attestry.ledger.domain.ledger.model.LedgerAppendInput;
import io.attestry.ledger.domain.ledger.model.LedgerChain;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.domain.ledger.model.LedgerPayloadMaterialized;
import io.attestry.ledger.domain.ledger.repository.LedgerChainRepository;
import io.attestry.ledger.domain.ledger.repository.LedgerChainRepository.AppendOutcome;
import io.attestry.ledger.domain.ledger.service.LedgerAppendDomainService;
import io.attestry.ledger.domain.ledger.service.LedgerHashService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerAppendService implements LedgerAppendUseCase {

    private static final Logger log = LoggerFactory.getLogger(LedgerAppendService.class);
    private static final int SCHEMA_VERSION = 1;

    private final LedgerChainRepository chainRepository;
    private final LedgerAppendDomainService appendDomainService;
    private final LedgerHashService hashService;
    private final Clock clock;
    private final Counter appendCounter;
    private final Counter duplicateCounter;
    private final Timer appendTimer;

    public LedgerAppendService(
        LedgerChainRepository chainRepository,
        LedgerAppendDomainService appendDomainService,
        LedgerHashService hashService,
        Clock clock,
        MeterRegistry meterRegistry
    ) {
        this.chainRepository = chainRepository;
        this.appendDomainService = appendDomainService;
        this.hashService = hashService;
        this.clock = clock;
        this.appendCounter = Counter.builder("ledger.append.count")
            .tag("duplicated", "false")
            .register(meterRegistry);
        this.duplicateCounter = Counter.builder("ledger.append.count")
            .tag("duplicated", "true")
            .register(meterRegistry);
        this.appendTimer = Timer.builder("ledger.append.duration")
            .register(meterRegistry);
    }

    @Override
    @Transactional
    public AppendLedgerEntryResult append(AppendLedgerEntryCommand command) {
        return appendTimer.record(() -> doAppend(command));
    }

    private AppendLedgerEntryResult doAppend(AppendLedgerEntryCommand command) {
        Instant occurredAt = command.occurredAt() == null ? Instant.now(clock) : command.occurredAt();
        LedgerAppendInput input = LedgerAppendInput.of(
            command.passportId(),
            command.eventCategory(),
            command.eventAction(),
            command.actorRole(),
            command.actorId(),
            occurredAt,
            command.payload(),
            command.idempotencyKey()
        );

        if (input.idempotencyKey() != null) {
            Optional<LedgerEntry> existing = chainRepository.findEntryByIdempotencyKey(input.idempotencyKey());
            if (existing.isPresent()) {
                duplicateCounter.increment();
                AppendLedgerEntryResult result = toResult(existing.get(), true);
                log.info("ledger append duplicated: passportId={}, ledgerId={}, seq={}, idempotencyKey={}",
                    result.passportId(), result.ledgerId(), result.seq(), result.idempotencyKey());
                return result;
            }
        }

        LedgerPayloadMaterialized materialized = appendDomainService.materialize(input);
        LedgerChain chain = chainRepository.loadForAppend(input.passportId().value());
        LedgerChain.AppendPlan appendPlan = chain.append(input, materialized, hashService, SCHEMA_VERSION);

        AppendOutcome outcome = chainRepository.saveAppend(appendPlan.entry(), appendPlan.nextChain());

        if (outcome.duplicated()) {
            duplicateCounter.increment();
        } else {
            appendCounter.increment();
        }

        AppendLedgerEntryResult result = toResult(outcome.entry(), outcome.duplicated());
        log.info("ledger append {}: passportId={}, ledgerId={}, seq={}, idempotencyKey={}",
            outcome.duplicated() ? "duplicated" : "success",
            result.passportId(), result.ledgerId(), result.seq(), result.idempotencyKey());
        return result;
    }

    private AppendLedgerEntryResult toResult(LedgerEntry entry, boolean duplicated) {
        return new AppendLedgerEntryResult(
            entry.ledgerId(),
            entry.passportId(),
            entry.seq(),
            entry.dataHash(),
            entry.prevHash(),
            entry.entryHash(),
            entry.idempotencyKey(),
            duplicated
        );
    }
}
