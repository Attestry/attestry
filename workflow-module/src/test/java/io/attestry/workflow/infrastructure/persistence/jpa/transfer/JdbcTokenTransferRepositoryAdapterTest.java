package io.attestry.workflow.infrastructure.persistence.jpa.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.AcceptCredential;
import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.model.TransferType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class JdbcTokenTransferRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");

    @Mock NamedParameterJdbcTemplate jdbcTemplate;
    @Mock JdbcOperations jdbcOperations;

    private JdbcTokenTransferRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JdbcTokenTransferRepositoryAdapter(jdbcTemplate);
        lenient().when(jdbcTemplate.getJdbcOperations()).thenReturn(jdbcOperations);
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

        when(jdbcOperations.update(anyString(), any(Object[].class)))
            .thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_token_transfers_pending_passport\""
            ));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () -> adapter.save(transfer));

        assertEquals(WorkflowErrorCode.TRANSFER_ALREADY_PENDING, ex.getErrorCode());
    }

    @Test
    void findLatestActivePendingByPassportId_withB2cConditions_bindsTransferTypeAndTenantId() {
        TokenTransfer pending = new TokenTransfer(
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

        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), anyRowMapper()))
            .thenReturn(List.of(pending));

        Optional<TokenTransfer> result = adapter.findLatestActivePendingByPassportId(
            "p1",
            NOW,
            TransferType.B2C,
            "tenant-1"
        );

        assertTrue(result.isPresent());
        assertEquals("t1", result.get().transferId());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(), anyRowMapper());

        String sql = sqlCaptor.getValue();
        MapSqlParameterSource params = paramsCaptor.getValue();

        assertTrue(sql.contains("transfer_type = :transferType"));
        assertTrue(sql.contains("tenant_id = :tenantId"));
        assertEquals("p1", params.getValue("passportId"));
        assertEquals("PENDING", params.getValue("status"));
        assertEquals("B2C", params.getValue("transferType"));
        assertEquals("tenant-1", params.getValue("tenantId"));
        assertEquals(Timestamp.from(NOW), params.getValue("now"));
    }

    @SuppressWarnings("unchecked")
    private static <T> RowMapper<T> anyRowMapper() {
        return (RowMapper<T>) any(RowMapper.class);
    }
}
