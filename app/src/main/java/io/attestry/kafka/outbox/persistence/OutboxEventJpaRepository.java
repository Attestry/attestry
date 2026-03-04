package io.attestry.kafka.outbox.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, String> {

    @Query("SELECT e FROM OutboxEventJpaEntity e " +
        "WHERE e.status = :status " +
        "AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) " +
        "ORDER BY e.createdAt ASC")
    List<OutboxEventJpaEntity> findRetryable(
        @Param("status") OutboxStatus status,
        @Param("now") Instant now,
        Pageable pageable
    );
}
