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
        "AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) " +
        "ORDER BY e.createdAt ASC")
    List<NotificationOutboxJpaEntity> findRetryable(
        @Param("status") NotificationOutboxStatus status,
        @Param("now") Instant now,
        Pageable pageable
    );
}
