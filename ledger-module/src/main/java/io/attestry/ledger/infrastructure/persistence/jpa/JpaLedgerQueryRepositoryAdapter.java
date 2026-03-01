package io.attestry.ledger.infrastructure.persistence.jpa;

import io.attestry.ledger.application.port.LedgerQueryRepositoryPort;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.infrastructure.persistence.jpa.entity.LedgerEntryJpaEntity;
import io.attestry.ledger.infrastructure.persistence.jpa.repository.LedgerEntryJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLedgerQueryRepositoryAdapter implements LedgerQueryRepositoryPort {

    private final LedgerEntryJpaRepository repository;

    public JpaLedgerQueryRepositoryAdapter(LedgerEntryJpaRepository repository) {
        this.repository = repository;
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
