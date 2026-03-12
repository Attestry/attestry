package io.attestry.workflow.infrastructure.persistence.jpa.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.AcceptCredential;
import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.infrastructure.persistence.jpa.transfer.entity.WorkflowTokenTransferJpaEntity;
import io.attestry.workflow.infrastructure.persistence.jpa.transfer.mapper.TokenTransferMapper;
import io.attestry.workflow.infrastructure.persistence.jpa.transfer.repository.TokenTransferJpaRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class JpaTokenTransferRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");

    @Mock TokenTransferJpaRepository repository;

    private JpaTokenTransferRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaTokenTransferRepositoryAdapter(repository, new TokenTransferMapper());
    }

    @Test
    void save_duplicatePendingConstraint_translatesToDomainError() {
        TokenTransfer transfer = TokenTransfer.createB2C(
            "t1",
            "p1",
            "tenant-1",
            AcceptCredential.ofQr("nonce-1"),
            NOW.plusSeconds(3600),
            NOW,
            "retail-user-1"
        );

        when(repository.save(org.mockito.ArgumentMatchers.any()))
            .thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_token_transfers_pending_passport\""
            ));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () -> adapter.save(transfer));

        assertEquals(WorkflowErrorCode.TRANSFER_ALREADY_PENDING, ex.getErrorCode());
    }

    @Test
    void findLatestActivePendingByPassportId_withB2cConditions_usesTypedRepositoryMethod() {
        WorkflowTokenTransferJpaEntity entity = new WorkflowTokenTransferJpaEntity(
            "t1",
            "p1",
            TransferType.B2C,
            TransferStatus.PENDING,
            AcceptMethod.QR,
            null,
            null,
            "tenant-1",
            "nonce-1",
            null,
            null,
            0,
            NOW.plusSeconds(3600),
            NOW.minusSeconds(60),
            "retail-user-1",
            null,
            null,
            null
        );
        when(repository.findFirstByPassportIdAndStatusAndExpiresAtAfterAndTransferTypeAndTenantIdOrderByCreatedAtDesc(
            "p1",
            TransferStatus.PENDING,
            NOW,
            TransferType.B2C,
            "tenant-1"
        )).thenReturn(Optional.of(entity));

        Optional<TokenTransfer> result = adapter.findLatestActivePendingByPassportId(
            "p1",
            NOW,
            TransferType.B2C,
            "tenant-1"
        );

        assertTrue(result.isPresent());
        assertEquals("t1", result.get().transferId());
        verify(repository).findFirstByPassportIdAndStatusAndExpiresAtAfterAndTransferTypeAndTenantIdOrderByCreatedAtDesc(
            "p1",
            TransferStatus.PENDING,
            NOW,
            TransferType.B2C,
            "tenant-1"
        );
    }
}
