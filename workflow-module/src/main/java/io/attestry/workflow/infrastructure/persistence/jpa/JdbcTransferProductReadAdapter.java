package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.application.port.PassportAuthorityQueryPort;
import io.attestry.workflow.application.port.TransferProductReadPort;
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
                SELECT pp.passport_id, pp.tenant_id, pa.asset_state, pa.risk_flag
                FROM product_passports pp
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
                WHERE pp.passport_id = ?
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
    public Optional<PassportAuthorityView> findPassportAuthority(String passportId) {
        return findPassportState(passportId)
            .map(state -> new PassportAuthorityView(
                state.passportId(),
                state.tenantId(),
                state.assetState(),
                state.riskFlag()
            ));
    }

    @Override
    public Optional<String> findCurrentOwnerId(String passportId) {
        List<String> rows = jdbcTemplate.query(
            "SELECT owner_id FROM passport_ownership WHERE passport_id = ?",
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
                FROM passport_permissions
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
