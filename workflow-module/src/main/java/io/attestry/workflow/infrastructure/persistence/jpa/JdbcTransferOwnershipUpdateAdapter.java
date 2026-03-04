package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.application.port.TransferOwnershipUpdatePort;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTransferOwnershipUpdateAdapter implements TransferOwnershipUpdatePort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTransferOwnershipUpdateAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
    }
}
