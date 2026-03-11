package io.attestry.workflow.infrastructure.persistence.jpa.transfer;

import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTokenTransferRepositoryAdapter implements TokenTransferRepository {

    private static final String PENDING_STATUS = TransferStatus.PENDING.name();

    private final JdbcTemplate jdbcTemplate;

    public JdbcTokenTransferRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TokenTransfer save(TokenTransfer transfer) {
        jdbcTemplate.update(
            """
                INSERT INTO token_transfers (
                    transfer_id, passport_id, transfer_type, status, accept_method,
                    from_owner_id, to_owner_id, tenant_id,
                    qr_nonce, code_hash, code_salt, attempt_count,
                    expires_at, created_at, created_by_user_id,
                    completed_at, cancelled_at, cancelled_by_user_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (transfer_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    to_owner_id = EXCLUDED.to_owner_id,
                    attempt_count = EXCLUDED.attempt_count,
                    completed_at = EXCLUDED.completed_at,
                    cancelled_at = EXCLUDED.cancelled_at,
                    cancelled_by_user_id = EXCLUDED.cancelled_by_user_id
            """,
            transfer.transferId(),
            transfer.passportId(),
            transfer.transferType().name(),
            transfer.status().name(),
            transfer.acceptMethod().name(),
            transfer.fromOwnerId(),
            transfer.toOwnerId(),
            transfer.tenantId(),
            transfer.qrNonce(),
            transfer.codeHash(),
            transfer.codeSalt(),
            transfer.attemptCount(),
            Timestamp.from(transfer.expiresAt()),
            Timestamp.from(transfer.createdAt()),
            transfer.createdByUserId(),
            transfer.completedAt() == null ? null : Timestamp.from(transfer.completedAt()),
            transfer.cancelledAt() == null ? null : Timestamp.from(transfer.cancelledAt()),
            transfer.cancelledByUserId()
        );
        return transfer;
    }

    @Override
    public Optional<TokenTransfer> findById(String transferId) {
        List<TokenTransfer> rows = jdbcTemplate.query(
            "SELECT * FROM token_transfers WHERE transfer_id = ?",
            (rs, rowNum) -> mapTransfer(rs),
            transferId
        );
        return rows.stream().findFirst();
    }

    @Override
    public Optional<TokenTransfer> findLatestActivePendingByPassportId(String passportId, Instant now) {
        List<TokenTransfer> rows = jdbcTemplate.query(
            """
                SELECT * FROM token_transfers
                WHERE passport_id = ?
                  AND status = ?
                  AND expires_at > ?
                ORDER BY created_at DESC
                LIMIT 1
            """,
            (rs, rowNum) -> mapTransfer(rs),
            passportId,
            PENDING_STATUS,
            Timestamp.from(now)
        );
        return rows.stream().findFirst();
    }

    @Override
    public boolean existsActivePendingByPassportId(String passportId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM token_transfers WHERE passport_id = ? AND status = ?",
            Integer.class,
            passportId,
            PENDING_STATUS
        );
        return count != null && count > 0;
    }

    @Override
    public List<TokenTransfer> findPendingExpiredBefore(Instant cutoff) {
        return jdbcTemplate.query(
            "SELECT * FROM token_transfers WHERE status = ? AND expires_at < ?",
            (rs, rowNum) -> mapTransfer(rs),
            PENDING_STATUS,
            Timestamp.from(cutoff)
        );
    }

    private TokenTransfer mapTransfer(ResultSet rs) throws SQLException {
        Timestamp completedAt = rs.getTimestamp("completed_at");
        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
        return new TokenTransfer(
            rs.getString("transfer_id"),
            rs.getString("passport_id"),
            TransferType.valueOf(rs.getString("transfer_type")),
            TransferStatus.valueOf(rs.getString("status")),
            AcceptMethod.valueOf(rs.getString("accept_method")),
            rs.getString("from_owner_id"),
            rs.getString("to_owner_id"),
            rs.getString("tenant_id"),
            rs.getString("qr_nonce"),
            rs.getString("code_hash"),
            rs.getString("code_salt"),
            rs.getInt("attempt_count"),
            rs.getTimestamp("expires_at").toInstant(),
            rs.getTimestamp("created_at").toInstant(),
            rs.getString("created_by_user_id"),
            completedAt == null ? null : completedAt.toInstant(),
            cancelledAt == null ? null : cancelledAt.toInstant(),
            rs.getString("cancelled_by_user_id")
        );
    }
}
