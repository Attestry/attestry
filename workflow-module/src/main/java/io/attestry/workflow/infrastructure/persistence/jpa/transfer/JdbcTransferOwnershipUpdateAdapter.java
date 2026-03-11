package io.attestry.workflow.infrastructure.persistence.jpa.transfer;

import io.attestry.workflow.application.port.transfer.TransferOwnershipUpdatePort;
import io.attestry.workflow.infrastructure.persistence.jpa.projection.WorkflowPassportProjectionWriter;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTransferOwnershipUpdateAdapter implements TransferOwnershipUpdatePort {

    private final JdbcTemplate jdbcTemplate;
    private final WorkflowPassportProjectionWriter projectionWriter;

    public JdbcTransferOwnershipUpdateAdapter(
        JdbcTemplate jdbcTemplate,
        WorkflowPassportProjectionWriter projectionWriter
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectionWriter = projectionWriter;
    }

    @Override
    public void upsertOwner(String passportId, String newOwnerId, Instant updatedAt) {
        jdbcTemplate.update(
            """
                INSERT INTO passport_ownership (passport_id, owner_id, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT (passport_id) DO UPDATE SET
                    owner_id = EXCLUDED.owner_id,
                    updated_at = EXCLUDED.updated_at
            """,
            passportId,
            newOwnerId,
            Timestamp.from(updatedAt)
        );

        projectionWriter.upsertOwnership(
            passportId,
            newOwnerId,
            "ownership:" + passportId + ":" + updatedAt.toEpochMilli(),
            null,
            updatedAt
        );
    }
}
