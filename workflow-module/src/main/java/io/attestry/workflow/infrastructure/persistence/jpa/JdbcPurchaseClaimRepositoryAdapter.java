package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.model.PurchaseClaimStatus;
import io.attestry.workflow.domain.claim.repository.PurchaseClaimRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPurchaseClaimRepositoryAdapter implements PurchaseClaimRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPurchaseClaimRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PurchaseClaim save(PurchaseClaim claim) {
        jdbcTemplate.update(
            """
                INSERT INTO purchase_claim_requests (
                    claim_id, tenant_id, group_id, claimant_user_id,
                    serial_number, model_name, evidence_group_id, note,
                    status, submitted_at,
                    reviewed_by_user_id, reviewed_at, rejection_reason,
                    passport_id, asset_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (claim_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    reviewed_by_user_id = EXCLUDED.reviewed_by_user_id,
                    reviewed_at = EXCLUDED.reviewed_at,
                    rejection_reason = EXCLUDED.rejection_reason,
                    passport_id = EXCLUDED.passport_id,
                    asset_id = EXCLUDED.asset_id
            """,
            claim.claimId(),
            claim.tenantId(),
            claim.groupId(),
            claim.claimantUserId(),
            claim.serialNumber(),
            claim.modelName(),
            claim.evidenceGroupId(),
            claim.note(),
            claim.status().name(),
            Timestamp.from(claim.submittedAt()),
            claim.reviewedByUserId(),
            claim.reviewedAt() == null ? null : Timestamp.from(claim.reviewedAt()),
            claim.rejectionReason(),
            claim.passportId(),
            claim.assetId(),
            Timestamp.from(claim.submittedAt())
        );
        return claim;
    }

    @Override
    public Optional<PurchaseClaim> findById(String claimId) {
        List<PurchaseClaim> rows = jdbcTemplate.query(
            "SELECT * FROM purchase_claim_requests WHERE claim_id = ?",
            (rs, rowNum) -> mapClaim(rs),
            claimId
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<PurchaseClaim> findByClaimantUserId(String userId) {
        return jdbcTemplate.query(
            "SELECT * FROM purchase_claim_requests WHERE claimant_user_id = ? ORDER BY submitted_at DESC",
            (rs, rowNum) -> mapClaim(rs),
            userId
        );
    }

    @Override
    public List<PurchaseClaim> findByTenantIdAndGroupIdAndStatus(String tenantId, String groupId, PurchaseClaimStatus status) {
        return jdbcTemplate.query(
            "SELECT * FROM purchase_claim_requests WHERE tenant_id = ? AND group_id = ? AND status = ? ORDER BY submitted_at ASC",
            (rs, rowNum) -> mapClaim(rs),
            tenantId, groupId, status.name()
        );
    }

    private PurchaseClaim mapClaim(ResultSet rs) throws SQLException {
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        return new PurchaseClaim(
            rs.getString("claim_id"),
            rs.getString("tenant_id"),
            rs.getString("group_id"),
            rs.getString("claimant_user_id"),
            rs.getString("serial_number"),
            rs.getString("model_name"),
            rs.getString("evidence_group_id"),
            rs.getString("note"),
            PurchaseClaimStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("submitted_at").toInstant(),
            rs.getString("reviewed_by_user_id"),
            reviewedAt == null ? null : reviewedAt.toInstant(),
            rs.getString("rejection_reason"),
            rs.getString("passport_id"),
            rs.getString("asset_id")
        );
    }
}
