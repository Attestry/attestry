package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.application.port.ShipmentEvidencePort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcShipmentEvidenceAdapter implements ShipmentEvidencePort {

    private static final String PLACEHOLDER_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final JdbcTemplate jdbcTemplate;

    public JdbcShipmentEvidenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createEvidenceGroupIfAbsent(
        String evidenceGroupId,
        String tenantId,
        String ownerUserId,
        Instant now
    ) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM workflow_shipment_evidence_groups WHERE evidence_group_id = ?",
            Integer.class,
            evidenceGroupId
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
            """
                INSERT INTO workflow_shipment_evidence_groups (
                    evidence_group_id, tenant_id, owner_user_id, created_at
                ) VALUES (?, ?, ?, ?)
            """,
            evidenceGroupId,
            tenantId,
            ownerUserId,
            Timestamp.from(now)
        );
    }

    @Override
    public void createPendingEvidence(
        String evidenceId,
        String evidenceGroupId,
        String objectKey,
        String originalFileName,
        String contentType,
        Instant now
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO workflow_shipment_evidences (
                    evidence_id,
                    evidence_group_id,
                    file_hash,
                    object_key,
                    original_file_name,
                    content_type,
                    status,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'PENDING_UPLOAD', ?)
            """,
            evidenceId,
            evidenceGroupId,
            PLACEHOLDER_HASH,
            objectKey,
            originalFileName,
            contentType,
            Timestamp.from(now)
        );
    }

    @Override
    public Optional<ShipmentEvidenceView> findEvidenceById(String evidenceGroupId, String evidenceId) {
        List<ShipmentEvidenceView> rows = jdbcTemplate.query(
            """
                SELECT evidence_id, evidence_group_id, file_hash, object_key, status
                FROM workflow_shipment_evidences
                WHERE evidence_group_id = ?
                  AND evidence_id = ?
            """,
            (rs, rowNum) -> mapEvidence(rs),
            evidenceGroupId,
            evidenceId
        );
        return rows.stream().findFirst();
    }

    @Override
    public void markEvidenceReady(String evidenceGroupId, String evidenceId, long sizeBytes, String fileHash, Instant now) {
        jdbcTemplate.update(
            """
                UPDATE workflow_shipment_evidences
                SET file_hash = ?,
                    size_bytes = ?,
                    status = 'READY',
                    completed_at = ?
                WHERE evidence_group_id = ?
                  AND evidence_id = ?
            """,
            fileHash,
            sizeBytes,
            Timestamp.from(now),
            evidenceGroupId,
            evidenceId
        );
    }

    @Override
    public Optional<EvidenceGroupScopeView> findEvidenceGroupScope(String evidenceGroupId) {
        List<EvidenceGroupScopeView> rows = jdbcTemplate.query(
            """
                SELECT evidence_group_id, tenant_id, owner_user_id
                FROM workflow_shipment_evidence_groups
                WHERE evidence_group_id = ?
            """,
            (rs, rowNum) -> new EvidenceGroupScopeView(
                rs.getString("evidence_group_id"),
                rs.getString("tenant_id"),
                rs.getString("owner_user_id")
            ),
            evidenceGroupId
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<String> findReadyEvidenceHashes(String evidenceGroupId) {
        return jdbcTemplate.query(
            """
                SELECT file_hash
                FROM workflow_shipment_evidences
                WHERE evidence_group_id = ?
                  AND status = 'READY'
                ORDER BY completed_at ASC, evidence_id ASC
            """,
            (rs, rowNum) -> rs.getString("file_hash"),
            evidenceGroupId
        );
    }

    @Override
    public List<ShipmentEvidenceView> findEvidenceByEvidenceGroupId(String evidenceGroupId) {
        return jdbcTemplate.query(
            """
                SELECT evidence_id, evidence_group_id, file_hash, object_key, status
                FROM workflow_shipment_evidences
                WHERE evidence_group_id = ?
                ORDER BY created_at ASC, evidence_id ASC
            """,
            (rs, rowNum) -> mapEvidence(rs),
            evidenceGroupId
        );
    }

    private ShipmentEvidenceView mapEvidence(ResultSet rs) throws SQLException {
        return new ShipmentEvidenceView(
            rs.getString("evidence_id"),
            rs.getString("evidence_group_id"),
            rs.getString("file_hash"),
            rs.getString("object_key"),
            rs.getString("status")
        );
    }
}
