package io.attestry.workflow.infrastructure.persistence.jpa.servicerequest;

import io.attestry.workflow.application.port.servicerequest.ServiceProductReadPort;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcServiceProductReadAdapter implements ServiceProductReadPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcServiceProductReadAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ServicePassportState> findPassportState(String passportId) {
        List<ServicePassportState> rows = jdbcTemplate.getJdbcOperations().query(
            """
                SELECT passport_id, tenant_id, asset_state, risk_flag
                FROM workflow_passport_state_projection
                WHERE passport_id = ?
            """,
            (rs, rowNum) -> new ServicePassportState(
                rs.getString("passport_id"),
                rs.getString("tenant_id"),
                rs.getString("asset_state"),
                rs.getString("risk_flag")
            ),
            passportId
        );
        return rows.stream().findFirst();
    }

    @Override
    public Optional<String> findCurrentOwnerId(String passportId) {
        List<String> rows = jdbcTemplate.getJdbcOperations().query(
            """
                SELECT owner_id FROM workflow_passport_ownership_projection
                WHERE passport_id = ?
            """,
            (rs, rowNum) -> rs.getString("owner_id"),
            passportId
        );
        return rows.stream().findFirst();
    }

    @Override
    public Optional<ServicePassportAssetInfo> findPassportAssetInfo(String passportId) {
        List<ServicePassportAssetInfo> rows = jdbcTemplate.getJdbcOperations().query(
            """
                SELECT passport_id, serial_number, model_name
                FROM workflow_passport_catalog_projection
                WHERE passport_id = ?
            """,
            (rs, rowNum) -> new ServicePassportAssetInfo(
                rs.getString("passport_id"),
                rs.getString("serial_number"),
                rs.getString("model_name")
            ),
            passportId
        );
        return rows.stream().findFirst();
    }
}
