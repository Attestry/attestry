package io.attestry.product.infrastructure.persistence.jdbc.query;

import io.attestry.product.application.port.query.PassportDistributionQueryPort;
import java.sql.Timestamp;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcPassportDistributionProjectionQueryAdapter implements PassportDistributionQueryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<DistributionRecord> findLatestDistribution(String passportId) {
        return jdbcTemplate.getJdbcOperations().query(
            """
                SELECT distribution_id,
                       target_tenant_id,
                       target_tenant_name,
                       target_tenant_type,
                       partner_link_id,
                       status,
                       distributed_at
                FROM product_passport_distribution_projection
                WHERE passport_id = ?
            """,
            rs -> rs.next()
                ? Optional.of(new DistributionRecord(
                    rs.getString("distribution_id"),
                    rs.getString("target_tenant_id"),
                    rs.getString("target_tenant_name"),
                    rs.getString("target_tenant_type"),
                    rs.getString("partner_link_id"),
                    rs.getString("status"),
                    toInstant(rs.getTimestamp("distributed_at"))
                ))
                : Optional.empty(),
            passportId
        );
    }

    private java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
