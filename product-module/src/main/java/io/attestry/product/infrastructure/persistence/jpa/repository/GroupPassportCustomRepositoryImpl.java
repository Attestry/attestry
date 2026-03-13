package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.application.port.query.GroupPassportQueryPort;
import io.attestry.product.application.port.query.GroupPassportQueryPort.GroupPassportRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GroupPassportCustomRepositoryImpl implements GroupPassportCustomRepository {

    private final EntityManager entityManager;

    @Override
    public GroupPassportQueryPort.PagedResult findByTenantWithFilters(
        String tenantId,
        int page,
        int size,
        String assetState,
        Instant createdFrom,
        Instant createdTo,
        String keyword
    ) {
        StringBuilder whereClause = new StringBuilder(" WHERE pp.tenant_id = :tenantId ");
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId", tenantId);

        if (assetState != null && !assetState.isBlank()) {
            whereClause.append(" AND pa.asset_state = :assetState ");
            params.put("assetState", assetState);
        }
        if (createdFrom != null) {
            whereClause.append(" AND pp.created_at >= :createdFrom ");
            params.put("createdFrom", Timestamp.from(createdFrom));
        }
        if (createdTo != null) {
            whereClause.append(" AND pp.created_at <= :createdTo ");
            params.put("createdTo", Timestamp.from(createdTo));
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" AND (LOWER(pa.serial_number) LIKE :keyword OR LOWER(pa.model_name) LIKE :keyword) ");
            params.put("keyword", like);
        }

        String fromClause = """
            FROM product_passports pp
            JOIN product_assets pa ON pa.asset_id = pp.asset_id
            """;

        Query countQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) " + fromClause + whereClause, Long.class);
        params.forEach(countQuery::setParameter);
        long total = (Long) countQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) total / size);

        Query contentQuery = entityManager.createNativeQuery(
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
                + " ORDER BY pp.created_at DESC ",
            Tuple.class);
        params.forEach(contentQuery::setParameter);
        contentQuery.setFirstResult(page * size);
        contentQuery.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Tuple> rows = contentQuery.getResultList();

        List<GroupPassportRow> content = rows.stream()
            .map(t -> {
                return new GroupPassportRow(
                    t.get("passport_id", String.class),
                    t.get("qr_public_code", String.class),
                    t.get("asset_id", String.class),
                    t.get("serial_number", String.class),
                    t.get("model_id", String.class),
                    t.get("model_name", String.class),
                    toInstant(t.get("manufactured_at")),
                    t.get("asset_state", String.class),
                    t.get("risk_flag", String.class),
                    t.get("owner_id", String.class),
                    toInstant(t.get("created_at"))
                );
            })
            .toList();

        return new GroupPassportQueryPort.PagedResult(content, page, size, total, totalPages);
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        throw new IllegalStateException("Unsupported temporal value type: " + value.getClass().getName());
    }
}
