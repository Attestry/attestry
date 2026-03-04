package io.attestry.ledger.infrastructure.persistence.jpa;

import io.attestry.ledger.domain.ledger.model.LedgerChain;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.domain.ledger.repository.LedgerChainRepository;
import io.attestry.ledger.infrastructure.persistence.jpa.entity.LedgerChainJpaEntity;
import io.attestry.ledger.infrastructure.persistence.jpa.entity.LedgerEntryJpaEntity;
import io.attestry.ledger.infrastructure.persistence.jpa.repository.LedgerChainJpaRepository;
import io.attestry.ledger.infrastructure.persistence.jpa.repository.LedgerEntryJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLedgerChainRepositoryAdapter implements LedgerChainRepository {

    private final LedgerChainJpaRepository chainRepository;
    private final LedgerEntryJpaRepository entryRepository;
    private final Clock clock;

    public JpaLedgerChainRepositoryAdapter(
        LedgerChainJpaRepository chainRepository,
        LedgerEntryJpaRepository entryRepository,
        Clock clock
    ) {
        this.chainRepository = chainRepository;
        this.entryRepository = entryRepository;
        this.clock = clock;
    }

    @Override
    public LedgerChain loadForAppend(String passportId) {
        LedgerChainJpaEntity lockedChain = lockOrCreateChain(passportId);
        return LedgerChain.restore(
            lockedChain.getPassportId(),
            lockedChain.getLastSeq(),
            lockedChain.getLastHash()
        );
    }

    @Override
    public AppendOutcome saveAppend(LedgerEntry newEntry, LedgerChain updatedChain) {
        try {
            LedgerEntryJpaEntity saved = entryRepository.save(toEntity(newEntry, Instant.now(clock)));
            chainRepository.save(new LedgerChainJpaEntity(
                updatedChain.state().passportId(),
                updatedChain.state().lastSeq(),
                updatedChain.state().lastHash()
            ));
            return new AppendOutcome(toDomain(saved), false);
        } catch (DataIntegrityViolationException ex) {
            if (newEntry.idempotencyKey() == null) {
                throw ex;
            }
            LedgerEntryJpaEntity existing = entryRepository.findByIdempotencyKey(newEntry.idempotencyKey())
                .orElseThrow(() -> ex);
            return new AppendOutcome(toDomain(existing), true);
        }
    }

    @Override
    public Optional<LedgerEntry> findEntryByIdempotencyKey(String idempotencyKey) {
        return entryRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    private LedgerChainJpaEntity lockOrCreateChain(String passportId) {
        Optional<LedgerChainJpaEntity> locked = chainRepository.findByPassportIdForUpdate(passportId);
        if (locked.isPresent()) {
            return locked.get();
        }
        try {
            LedgerChain initialized = LedgerChain.initialize(passportId);
            chainRepository.save(new LedgerChainJpaEntity(
                initialized.state().passportId(),
                initialized.state().lastSeq(),
                initialized.state().lastHash()
            ));
        } catch (DataIntegrityViolationException ignored) {
            // concurrent create; fetch with lock below
        }
        return chainRepository.findByPassportIdForUpdate(passportId)
            .orElseThrow(() -> new IllegalStateException("failed to initialize ledger chain"));
    }

    private LedgerEntryJpaEntity toEntity(LedgerEntry entry, Instant createdAt) {
        return new LedgerEntryJpaEntity(
            entry.ledgerId(),
            entry.passportId(),
            entry.seq(),
            entry.eventCategory(),
            entry.eventAction(),
            entry.actorRole(),
            entry.actorId(),
            entry.occurredAt(),
            createdAt,
            entry.payloadJson(),
            entry.payloadCanonical(),
            entry.dataHash(),
            entry.prevHash(),
            entry.entryHash(),
            entry.idempotencyKey(),
            entry.schemaVersion()
        );
    }

    private LedgerEntry toDomain(LedgerEntryJpaEntity entity) {
        return LedgerEntry.rehydrate(
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
