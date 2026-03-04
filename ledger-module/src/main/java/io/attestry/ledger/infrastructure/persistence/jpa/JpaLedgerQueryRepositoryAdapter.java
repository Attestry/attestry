package io.attestry.ledger.infrastructure.persistence.jpa;

import io.attestry.ledger.application.port.LedgerQueryRepositoryPort;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.infrastructure.persistence.jpa.entity.LedgerEntryJpaEntity;
import io.attestry.ledger.infrastructure.persistence.jpa.repository.LedgerChainJpaRepository;
import io.attestry.ledger.infrastructure.persistence.jpa.repository.LedgerEntryJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLedgerQueryRepositoryAdapter implements LedgerQueryRepositoryPort {

    private final LedgerEntryJpaRepository repository;
    private final LedgerChainJpaRepository chainRepository;

    public JpaLedgerQueryRepositoryAdapter(
        LedgerEntryJpaRepository repository,
        LedgerChainJpaRepository chainRepository
    ) {
        this.repository = repository;
        this.chainRepository = chainRepository;
    }

    @Override
    public List<LedgerEntry> findByPassportIdOrderBySeqAsc(String passportId) {
        return repository.findByPassportIdOrderBySeqAsc(passportId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public Optional<LedgerEntry> findByPassportIdAndLedgerId(String passportId, String ledgerId) {
        return repository.findByPassportIdAndLedgerId(passportId, ledgerId)
            .map(this::toDomain);
    }

    @Override
    public List<String> findAllPassportIds() {
        return chainRepository.findAllPassportIds();
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
