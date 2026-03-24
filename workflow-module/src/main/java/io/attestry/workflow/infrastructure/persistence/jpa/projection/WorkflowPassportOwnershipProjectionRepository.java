package io.attestry.workflow.infrastructure.persistence.jpa.projection;

import java.sql.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class WorkflowPassportOwnershipProjectionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    void upsertOwnership(
        String passportId,
        String ownerId,
        String sourceEventId,
        Long sourceEventVersion,
        Timestamp updatedAt
    ) {
        upsertOwnershipProjection(passportId, ownerId, sourceEventId, sourceEventVersion, updatedAt);
        updateStateProjectionOwner(passportId, ownerId, sourceEventId, sourceEventVersion, updatedAt);
    }

    private void upsertOwnershipProjection(
        String passportId,
        String ownerId,
        String sourceEventId,
        Long sourceEventVersion,
        Timestamp updatedAt
    ) {
        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO workflow_passport_ownership_projection (
                    passport_id,
                    owner_id,
                    source_event_id,
                    source_event_version,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (passport_id) DO UPDATE SET
                    owner_id = EXCLUDED.owner_id,
                    source_event_id = EXCLUDED.source_event_id,
                    source_event_version = EXCLUDED.source_event_version,
                    updated_at = EXCLUDED.updated_at
            """,
            passportId,
            ownerId,
            sourceEventId,
            sourceEventVersion,
            updatedAt
        );
    }

    private void updateStateProjectionOwner(
        String passportId,
        String ownerId,
        String sourceEventId,
        Long sourceEventVersion,
        Timestamp updatedAt
    ) {
        jdbcTemplate.getJdbcOperations().update(
            """
                UPDATE workflow_passport_state_projection
                SET current_owner_id = ?,
                    source_event_id = ?,
                    source_event_version = ?,
                    updated_at = ?
                WHERE passport_id = ?
            """,
            ownerId,
            sourceEventId,
            sourceEventVersion,
            updatedAt,
            passportId
        );
    }
}
