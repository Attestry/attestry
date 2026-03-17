package io.attestry.workflow.infrastructure.persistence.jpa.manual;

import io.attestry.workflow.application.port.manual.PassportManualReadPort;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPassportManualReadAdapter implements PassportManualReadPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcPassportManualReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PassportManualContext> findContext(String passportId) {
        List<PassportManualContext> rows = jdbcTemplate.getJdbcOperations().query(
            """
                SELECT state.passport_id,
                       state.tenant_id,
                       catalog.serial_number,
                       catalog.model_name,
                       owner.owner_id
                FROM workflow_passport_state_projection state
                JOIN workflow_passport_catalog_projection catalog
                  ON catalog.passport_id = state.passport_id
                LEFT JOIN workflow_passport_ownership_projection owner
                  ON owner.passport_id = state.passport_id
                WHERE state.passport_id = ?
            """,
            (rs, rowNum) -> new PassportManualContext(
                rs.getString("passport_id"),
                rs.getString("tenant_id"),
                rs.getString("serial_number"),
                rs.getString("model_name"),
                rs.getString("owner_id")
            ),
            passportId
        );
        return rows.stream().findFirst();
    }
}
