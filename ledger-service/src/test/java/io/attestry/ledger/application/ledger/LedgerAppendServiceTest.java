package io.attestry.ledger.application.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.ledger.application.ledger.command.AppendLedgerEntryCommand;
import io.attestry.ledger.application.ledger.result.AppendLedgerEntryResult;
import io.attestry.ledger.domain.ledger.model.LedgerAppendInput;
import io.attestry.ledger.domain.ledger.model.LedgerChain;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.domain.ledger.model.LedgerPayloadMaterialized;
import io.attestry.ledger.domain.ledger.repository.LedgerChainRepository;
import io.attestry.ledger.domain.ledger.repository.LedgerChainRepository.AppendOutcome;
import io.attestry.ledger.domain.ledger.service.LedgerAppendDomainService;
import io.attestry.ledger.domain.ledger.service.LedgerHashService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LedgerAppendServiceTest {

    private static final String PASSPORT_ID = "passport-1";
    private static final Instant FIXED_NOW = Instant.parse("2026-06-15T12:00:00Z");
    private static final Instant COMMAND_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private LedgerChainRepository chainRepository;

    @Mock
    private LedgerAppendDomainService appendDomainService;

    @Mock
    private LedgerHashService hashService;

    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private LedgerAppendService service;

    @BeforeEach
    void setUp() {
        service = new LedgerAppendService(
            chainRepository, appendDomainService, hashService, clock, meterRegistry
        );
    }

    @Test
    void append_success() {
        AppendLedgerEntryCommand command = new AppendLedgerEntryCommand(
            PASSPORT_ID, "LIFECYCLE", "MINTED", "BRAND", "actor-1",
            COMMAND_TIME, Map.of("key", "value"), "idem-1"
        );

        LedgerPayloadMaterialized materialized = new LedgerPayloadMaterialized(
            "{\"key\":\"value\"}", "{\"key\":\"value\"}", "data-hash-1"
        );
        when(appendDomainService.materialize(any(LedgerAppendInput.class))).thenReturn(materialized);

        LedgerChain chain = LedgerChain.initialize(PASSPORT_ID);
        when(chainRepository.loadForAppend(PASSPORT_ID)).thenReturn(chain);
        when(chainRepository.findEntryByIdempotencyKey("idem-1")).thenReturn(Optional.empty());

        LedgerEntry savedEntry = LedgerEntry.rehydrate(
            "ledger-id-1", PASSPORT_ID, 1L,
            "LIFECYCLE", "MINTED", "BRAND", "actor-1",
            COMMAND_TIME, "{\"key\":\"value\"}", "{\"key\":\"value\"}", "data-hash-1",
            null, "entry-hash-1", "idem-1", 1
        );
        when(chainRepository.saveAppend(any(LedgerEntry.class), any(LedgerChain.class)))
            .thenReturn(new AppendOutcome(savedEntry, false));

        when(hashService.entryHash(any(), any(), any(long.class), any(), any(), any(), any(), any()))
            .thenReturn("entry-hash-1");

        AppendLedgerEntryResult result = service.append(command);

        assertEquals("ledger-id-1", result.ledgerId());
        assertEquals(PASSPORT_ID, result.passportId());
        assertEquals(1L, result.seq());
        assertEquals("data-hash-1", result.dataHash());
        assertEquals("entry-hash-1", result.entryHash());
        assertEquals("idem-1", result.idempotencyKey());
        assertFalse(result.duplicated());
    }

    @Test
    void append_duplicateIdempotencyKey() {
        AppendLedgerEntryCommand command = new AppendLedgerEntryCommand(
            PASSPORT_ID, "LIFECYCLE", "MINTED", "BRAND", "actor-1",
            COMMAND_TIME, Map.of("key", "value"), "idem-dup"
        );

        LedgerEntry existingEntry = LedgerEntry.rehydrate(
            "existing-ledger-id", PASSPORT_ID, 1L,
            "LIFECYCLE", "MINTED", "BRAND", "actor-1",
            COMMAND_TIME, "{\"key\":\"value\"}", "{\"key\":\"value\"}", "data-hash-1",
            null, "entry-hash-1", "idem-dup", 1
        );
        when(chainRepository.findEntryByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existingEntry));

        AppendLedgerEntryResult result = service.append(command);

        assertTrue(result.duplicated());
        assertEquals("existing-ledger-id", result.ledgerId());
        assertEquals(PASSPORT_ID, result.passportId());
        assertEquals(1L, result.seq());
        assertEquals("idem-dup", result.idempotencyKey());
    }

    @Test
    void append_defaultsOccurredAtToClock() {
        AppendLedgerEntryCommand command = new AppendLedgerEntryCommand(
            PASSPORT_ID, "LIFECYCLE", "MINTED", "BRAND", "actor-1",
            null, Map.of("key", "value"), null
        );

        LedgerPayloadMaterialized materialized = new LedgerPayloadMaterialized(
            "{\"key\":\"value\"}", "{\"key\":\"value\"}", "data-hash-1"
        );
        when(appendDomainService.materialize(any(LedgerAppendInput.class))).thenReturn(materialized);

        LedgerChain chain = LedgerChain.initialize(PASSPORT_ID);
        when(chainRepository.loadForAppend(PASSPORT_ID)).thenReturn(chain);

        LedgerEntry savedEntry = LedgerEntry.rehydrate(
            "ledger-id-2", PASSPORT_ID, 1L,
            "LIFECYCLE", "MINTED", "BRAND", "actor-1",
            FIXED_NOW, "{\"key\":\"value\"}", "{\"key\":\"value\"}", "data-hash-1",
            null, "entry-hash-2", null, 1
        );
        when(chainRepository.saveAppend(any(LedgerEntry.class), any(LedgerChain.class)))
            .thenReturn(new AppendOutcome(savedEntry, false));

        when(hashService.entryHash(any(), any(), any(long.class), any(), any(), any(), any(), eq(FIXED_NOW)))
            .thenReturn("entry-hash-2");

        AppendLedgerEntryResult result = service.append(command);

        assertFalse(result.duplicated());
        assertEquals("ledger-id-2", result.ledgerId());
    }
}
