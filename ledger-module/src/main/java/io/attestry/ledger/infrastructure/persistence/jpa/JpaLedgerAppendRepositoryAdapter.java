package io.attestry.ledger.infrastructure.persistence.jpa;

import io.attestry.ledger.application.port.LedgerAppendRepositoryPort;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.domain.ledger.service.LedgerHashService;
import io.attestry.ledger.infrastructure.persistence.jpa.entity.LedgerChainJpaEntity;
import io.attestry.ledger.infrastructure.persistence.jpa.entity.LedgerEntryJpaEntity;
import io.attestry.ledger.infrastructure.persistence.jpa.repository.LedgerChainJpaRepository;
import io.attestry.ledger.infrastructure.persistence.jpa.repository.LedgerEntryJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLedgerAppendRepositoryAdapter implements LedgerAppendRepositoryPort {

    private static final int SCHEMA_VERSION = 1;

    private final LedgerChainJpaRepository chainRepository;
    private final LedgerEntryJpaRepository entryRepository;
    private final LedgerHashService hashService;
    private final Clock clock;

    public JpaLedgerAppendRepositoryAdapter(
        LedgerChainJpaRepository chainRepository,
        LedgerEntryJpaRepository entryRepository,
        LedgerHashService hashService,
        Clock clock
    ) {
        this.chainRepository = chainRepository;
        this.entryRepository = entryRepository;
        this.hashService = hashService;
        this.clock = clock;
    }

    @Override
    public AppendOutcome append(AppendRequest request) {
        if (request.idempotencyKey() != null) {
            Optional<LedgerEntryJpaEntity> existing = entryRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                return new AppendOutcome(toDomain(existing.get()), true);
            }
        }

        LedgerChainJpaEntity chain = lockOrCreateChain(request.passportId());
        long lastSeq = chain.getLastSeq() == null ? 0L : chain.getLastSeq();
        long newSeq = lastSeq + 1L;
        String prevHash = chain.getLastHash();

        String entryHash = hashService.entryHash(
            prevHash,
            request.dataHash(),
            newSeq,
            request.eventCategory(),
            request.eventAction(),
            request.actorRole(),
            request.actorId(),
            request.occurredAt()
        );

        LedgerEntryJpaEntity candidate = new LedgerEntryJpaEntity(
            UUID.randomUUID().toString(),
            request.passportId(),
            newSeq,
            request.eventCategory(),
            request.eventAction(),
            request.actorRole(),
            request.actorId(),
            request.occurredAt(),
            Instant.now(clock),
            request.payloadJson(),
            request.payloadCanonical(),
            request.dataHash(),
            prevHash,
            entryHash,
            request.idempotencyKey(),
            SCHEMA_VERSION
        );

        try {
            LedgerEntryJpaEntity saved = entryRepository.save(candidate);
            chainRepository.save(new LedgerChainJpaEntity(chain.getPassportId(), newSeq, entryHash));
            return new AppendOutcome(toDomain(saved), false);
        } catch (DataIntegrityViolationException ex) {
            if (request.idempotencyKey() == null) {
                throw ex;
            }
            LedgerEntryJpaEntity existing = entryRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElseThrow(() -> ex);
            return new AppendOutcome(toDomain(existing), true);
        }
    }

    private LedgerChainJpaEntity lockOrCreateChain(String passportId) {
        Optional<LedgerChainJpaEntity> locked = chainRepository.findByPassportIdForUpdate(passportId);
        if (locked.isPresent()) {
            return locked.get();
        }
        try {
            chainRepository.save(new LedgerChainJpaEntity(passportId, 0L, null));
        } catch (DataIntegrityViolationException ignored) {
            // concurrent create; fetch with lock below
        }
        return chainRepository.findByPassportIdForUpdate(passportId)
            .orElseThrow(() -> new IllegalStateException("failed to initialize ledger chain"));
    }

    private LedgerEntry toDomain(LedgerEntryJpaEntity entity) {
        return new LedgerEntry(
            entity.getLedgerId(),
            entity.getPassportId(),
            entity.getSeq(),
            entity.getEventCategory(),
            entity.getEventAction(),
            entity.getActorRole(),
            entity.getActorId(),
            entity.getOccurredAt(),
            entity.getPayloadJson(),
            entity.getPayloadCanonical(),
            entity.getDataHash(),
            entity.getPrevHash(),
            entity.getEntryHash(),
            entity.getIdempotencyKey(),
            entity.getSchemaVersion()
        );
    }
}
