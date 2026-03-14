package io.attestry.ledger.infrastructure.persistence.jpa.repository;

import io.attestry.ledger.infrastructure.persistence.jpa.entity.LedgerEntryJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, String> {
    Optional<LedgerEntryJpaEntity> findByIdempotencyKey(String idempotencyKey);

    List<LedgerEntryJpaEntity> findByPassportIdOrderBySeqAsc(String passportId);

    Optional<LedgerEntryJpaEntity> findByPassportIdAndLedgerId(String passportId, String ledgerId);
}
