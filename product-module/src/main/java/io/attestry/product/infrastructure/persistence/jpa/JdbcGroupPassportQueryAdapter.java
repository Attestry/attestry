package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.application.port.GroupPassportQueryPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcGroupPassportQueryAdapter implements GroupPassportQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcGroupPassportQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PagedResult findByTenant(
        String tenantId,
        int page,
        int size,
        String assetState,
        Instant createdFrom,
        Instant createdTo,
        String keyword
    ) {
        StringBuilder whereClause = new StringBuilder(" WHERE pp.tenant_id = ? ");
        List<Object> filterParams = new ArrayList<>();
        filterParams.add(tenantId);
        if (assetState != null && !assetState.isBlank()) {
            whereClause.append(" AND pa.asset_state = ? ");
            filterParams.add(assetState);
        }
        if (createdFrom != null) {
            whereClause.append(" AND pp.created_at >= ? ");
            filterParams.add(Timestamp.from(createdFrom));
        }
        if (createdTo != null) {
            whereClause.append(" AND pp.created_at <= ? ");
            filterParams.add(Timestamp.from(createdTo));
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" AND (LOWER(pa.serial_number) LIKE ? OR LOWER(pa.model_name) LIKE ?) ");
            filterParams.add(like);
            filterParams.add(like);
        }

        Long totalElements = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM product_passports pp
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
                """
                + whereClause,
            Long.class,
            filterParams.toArray()
        );
        long total = totalElements != null ? totalElements : 0;
        int totalPages = (int) Math.ceil((double) total / size);

        List<Object> contentParams = new ArrayList<>(filterParams);
        contentParams.add(size);
        contentParams.add(page * size);

        List<GroupPassportView> content = jdbcTemplate.query(
            """
                SELECT pp.passport_id, pp.qr_public_code,
                       pa.asset_id, pa.serial_number, pa.model_id, pa.model_name,
                       pa.manufactured_at, pa.asset_state, pa.risk_flag,
                       po.owner_id,
                       pp.created_at
                FROM product_passports pp
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
                LEFT JOIN passport_ownership po ON po.passport_id = pp.passport_id
                """
                + whereClause
                + """
                ORDER BY pp.created_at DESC
                LIMIT ? OFFSET ?
                """,
            (rs, rowNum) -> {
                Timestamp mfgAt = rs.getTimestamp("manufactured_at");
                return new GroupPassportView(
                    rs.getString("passport_id"),
                    rs.getString("qr_public_code"),
                    rs.getString("asset_id"),
                    rs.getString("serial_number"),
                    rs.getString("model_id"),
                    rs.getString("model_name"),
                    mfgAt != null ? mfgAt.toInstant() : null,
                    rs.getString("asset_state"),
                    rs.getString("risk_flag"),
                    rs.getString("owner_id"),
                    rs.getTimestamp("created_at").toInstant()
                );
            },
            contentParams.toArray()
        );

        return new PagedResult(content, page, size, total, totalPages);
    }
}
