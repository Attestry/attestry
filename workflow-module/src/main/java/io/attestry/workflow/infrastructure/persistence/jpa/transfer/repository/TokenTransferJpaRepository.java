package io.attestry.workflow.infrastructure.persistence.jpa.transfer.repository;

import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.infrastructure.persistence.jpa.transfer.entity.WorkflowTokenTransferJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenTransferJpaRepository extends JpaRepository<WorkflowTokenTransferJpaEntity, String> {

    Optional<WorkflowTokenTransferJpaEntity> findFirstByPassportIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
        String passportId,
        TransferStatus status,
        Instant now
    );

    Optional<WorkflowTokenTransferJpaEntity> findFirstByPassportIdAndStatusAndExpiresAtAfterAndTransferTypeAndTenantIdOrderByCreatedAtDesc(
        String passportId,
        TransferStatus status,
        Instant now,
        TransferType transferType,
        String tenantId
    );

    boolean existsByPassportIdAndStatus(String passportId, TransferStatus status);

    List<WorkflowTokenTransferJpaEntity> findByStatusAndExpiresAtBefore(TransferStatus status, Instant cutoff);
}
