package io.attestry.workflow.infrastructure.persistence.jpa.transfer;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import io.attestry.workflow.infrastructure.persistence.jpa.transfer.mapper.TokenTransferMapper;
import io.attestry.workflow.infrastructure.persistence.jpa.transfer.repository.TokenTransferJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaTokenTransferRepositoryAdapter implements TokenTransferRepository {

    private static final String DUPLICATE_PENDING_CONSTRAINT = "uq_token_transfers_pending_passport";

    private final TokenTransferJpaRepository repository;
    private final TokenTransferMapper mapper;

    @Override
    public TokenTransfer save(TokenTransfer transfer) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(transfer)));
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicatePendingConstraint(ex)) {
                throw new WorkflowDomainException(
                    WorkflowErrorCode.TRANSFER_ALREADY_PENDING,
                    "A pending transfer already exists for this passport"
                );
            }
            throw ex;
        }
    }

    @Override
    public Optional<TokenTransfer> findById(String transferId) {
        return repository.findById(transferId).map(mapper::toDomain);
    }

    @Override
    public Optional<TokenTransfer> findLatestActivePendingByPassportId(String passportId, Instant now) {
        return repository.findFirstByPassportIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            passportId,
            TransferStatus.PENDING,
            now
        ).map(mapper::toDomain);
    }

    @Override
    public Optional<TokenTransfer> findLatestActivePendingByPassportId(
        String passportId,
        Instant now,
        TransferType transferType,
        String tenantId
    ) {
        if (transferType == null) {
            return findLatestActivePendingByPassportId(passportId, now);
        }
        if (tenantId == null || tenantId.isBlank()) {
            return repository.findFirstByPassportIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                passportId,
                TransferStatus.PENDING,
                now
            )
                .filter(entity -> entity.getTransferType() == transferType)
                .map(mapper::toDomain);
        }
        return repository.findFirstByPassportIdAndStatusAndExpiresAtAfterAndTransferTypeAndTenantIdOrderByCreatedAtDesc(
            passportId,
            TransferStatus.PENDING,
            now,
            transferType,
            tenantId
        ).map(mapper::toDomain);
    }

    @Override
    public boolean existsActivePendingByPassportId(String passportId) {
        return repository.existsByPassportIdAndStatus(passportId, TransferStatus.PENDING);
    }

    @Override
    public List<TokenTransfer> findPendingExpiredBefore(Instant cutoff) {
        return repository.findByStatusAndExpiresAtBefore(TransferStatus.PENDING, cutoff)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    private boolean isDuplicatePendingConstraint(DataIntegrityViolationException ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        String message = root != null ? root.getMessage() : ex.getMessage();
        return message != null && message.contains(DUPLICATE_PENDING_CONSTRAINT);
    }
}
