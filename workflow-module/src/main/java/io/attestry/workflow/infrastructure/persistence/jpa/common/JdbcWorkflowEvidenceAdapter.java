package io.attestry.workflow.infrastructure.persistence.jpa.common;

import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWorkflowEvidenceAdapter implements WorkflowEvidencePort {

    private static final String PLACEHOLDER_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcWorkflowEvidenceAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createEvidenceGroupIfAbsent(
        String evidenceGroupId,
        String tenantId,
        String ownerUserId,
        Instant now
    ) {
        Integer count = jdbcTemplate.getJdbcOperations().queryForObject(
            "SELECT COUNT(1) FROM workflow_evidence_groups WHERE evidence_group_id = ?",
            Integer.class,
            evidenceGroupId
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO workflow_evidence_groups (
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
        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO workflow_evidences (
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
    public Optional<EvidenceRecord> findEvidenceById(String evidenceGroupId, String evidenceId) {
        List<EvidenceRecord> rows = jdbcTemplate.getJdbcOperations().query(
            """
                SELECT evidence_id, evidence_group_id, file_hash, object_key,
                       original_file_name, content_type, COALESCE(size_bytes, 0) AS size_bytes, status
                FROM workflow_evidences
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
        jdbcTemplate.getJdbcOperations().update(
            """
                UPDATE workflow_evidences
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
    public Optional<EvidenceGroupScopeRecord> findEvidenceGroupScope(String evidenceGroupId) {
        List<EvidenceGroupScopeRecord> rows = jdbcTemplate.getJdbcOperations().query(
            """
                SELECT evidence_group_id, tenant_id, owner_user_id
                FROM workflow_evidence_groups
                WHERE evidence_group_id = ?
            """,
            (rs, rowNum) -> new EvidenceGroupScopeRecord(
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
        return jdbcTemplate.getJdbcOperations().query(
            """
                SELECT file_hash
                FROM workflow_evidences
                WHERE evidence_group_id = ?
                  AND status = 'READY'
                ORDER BY completed_at ASC, evidence_id ASC
            """,
            (rs, rowNum) -> rs.getString("file_hash"),
            evidenceGroupId
        );
    }

    @Override
    public List<EvidenceRecord> findEvidenceByEvidenceGroupId(String evidenceGroupId) {
        return jdbcTemplate.getJdbcOperations().query(
            """
                SELECT evidence_id, evidence_group_id, file_hash, object_key,
                       original_file_name, content_type, COALESCE(size_bytes, 0) AS size_bytes, status
                FROM workflow_evidences
                WHERE evidence_group_id = ?
                ORDER BY created_at ASC, evidence_id ASC
            """,
            (rs, rowNum) -> mapEvidence(rs),
            evidenceGroupId
        );
    }

    private EvidenceRecord mapEvidence(ResultSet rs) throws SQLException {
        return new EvidenceRecord(
            rs.getString("evidence_id"),
            rs.getString("evidence_group_id"),
            rs.getString("file_hash"),
            rs.getString("object_key"),
            rs.getString("original_file_name"),
            rs.getString("content_type"),
            rs.getLong("size_bytes"),
            rs.getString("status")
        );
    }
}
