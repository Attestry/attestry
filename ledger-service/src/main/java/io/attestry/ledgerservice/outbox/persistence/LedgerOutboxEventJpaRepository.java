package io.attestry.ledgerservice.outbox.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerOutboxEventJpaRepository extends JpaRepository<LedgerOutboxEventJpaEntity, String> {

    @Query("SELECT e FROM LedgerOutboxEventJpaEntity e " +
        "WHERE e.eventType = 'LEDGER_APPEND' " +
        "AND e.status = :status " +
        "AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) " +
        "ORDER BY e.createdAt ASC")
    List<LedgerOutboxEventJpaEntity> findRetryable(
        @Param("status") LedgerOutboxStatus status,
        @Param("now") Instant now,
        Pageable pageable
    );

    long countByEventTypeAndStatus(String eventType, LedgerOutboxStatus status);

    Optional<LedgerOutboxEventJpaEntity> findFirstByEventTypeAndStatusOrderByCreatedAtAsc(
        String eventType,
        LedgerOutboxStatus status
    );

    @Modifying
    @Query("DELETE FROM LedgerOutboxEventJpaEntity e " +
        "WHERE e.eventType = :eventType AND e.status = :status AND e.publishedAt < :before")
    int deleteByEventTypeAndStatusAndPublishedBefore(
        @Param("eventType") String eventType,
        @Param("status") LedgerOutboxStatus status,
        @Param("before") Instant before
    );
}
