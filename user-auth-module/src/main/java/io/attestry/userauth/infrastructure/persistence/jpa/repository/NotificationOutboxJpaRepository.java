package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.domain.membership.model.NotificationOutboxStatus;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.NotificationOutboxJpaEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationOutboxJpaRepository extends JpaRepository<NotificationOutboxJpaEntity, String> {

    @Query("SELECT e FROM NotificationOutboxJpaEntity e " +
        "WHERE e.status = :status " +
        "AND e.processingStartedAt IS NOT NULL " +
        "AND e.processingStartedAt < :threshold")
    List<NotificationOutboxJpaEntity> findTimedOutProcessing(
        @Param("status") NotificationOutboxStatus status,
        @Param("threshold") Instant threshold
    );

    @Query("SELECT e FROM NotificationOutboxJpaEntity e WHERE e.id IN :ids")
    List<NotificationOutboxJpaEntity> findRetryable(
        @Param("ids") List<String> ids
    );
}
