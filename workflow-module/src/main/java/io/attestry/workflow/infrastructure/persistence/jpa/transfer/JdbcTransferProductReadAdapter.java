package io.attestry.workflow.infrastructure.persistence.jpa.transfer;

import io.attestry.workflow.application.port.delegation.PassportAuthorityQueryPort;
import io.attestry.workflow.application.port.transfer.TransferProductReadPort;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTransferProductReadAdapter implements TransferProductReadPort, PassportAuthorityQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTransferProductReadAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<TransferPassportState> findPassportState(String passportId) {
        List<TransferPassportState> rows = jdbcTemplate.query(
            """
                SELECT passport_id, tenant_id, asset_state, risk_flag
                FROM workflow_passport_state_projection
                WHERE passport_id = ?
            """,
            (rs, rowNum) -> new TransferPassportState(
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
    public Optional<PassportAuthorityRecord> findPassportAuthority(String passportId) {
        return findPassportState(passportId)
            .map(state -> new PassportAuthorityRecord(
                state.passportId(),
                state.tenantId(),
                state.assetState(),
                state.riskFlag()
            ));
    }

    @Override
    public Optional<String> findCurrentOwnerId(String passportId) {
        List<String> rows = jdbcTemplate.query(
            "SELECT owner_id FROM workflow_passport_ownership_projection WHERE passport_id = ?",
            (rs, rowNum) -> rs.getString("owner_id"),
            passportId
        );
        return rows.stream().findFirst();
    }

    @Override
    public boolean hasRetailPermission(String passportId, String sellerTenantId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(1)
                FROM workflow_passport_permission_projection
                WHERE passport_id = ?
                  AND seller_tenant_id = ?
                  AND status = 'ACTIVE'
            """,
            Integer.class,
            passportId,
            sellerTenantId
        );
        return count != null && count > 0;
    }
}
