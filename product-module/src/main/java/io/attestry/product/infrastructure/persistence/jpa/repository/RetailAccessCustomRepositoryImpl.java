package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.application.port.projection.ProductRetailAccessProjectionPort.PagedRetailAccessResult;
import io.attestry.product.application.port.projection.ProductRetailAccessProjectionPort.RetailAccessRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RetailAccessCustomRepositoryImpl implements RetailAccessCustomRepository {

    private final EntityManager entityManager;

    @Override
    public PagedRetailAccessResult findAccessiblePassportsWithFilters(
        String tenantId,
        int page,
        int size,
        String keyword,
        String sourceTenantId
    ) {
        StringBuilder whereClause = new StringBuilder("""
            WHERE prap.tenant_id = :tenantId
              AND prap.access_status = 'ACTIVE'
        """);
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId", tenantId);

        if (sourceTenantId != null && !sourceTenantId.isBlank()) {
            whereClause.append(" AND prap.source_tenant_id = :sourceTenantId ");
            params.put("sourceTenantId", sourceTenantId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" AND (LOWER(pa.serial_number) LIKE :keyword OR LOWER(pa.model_name) LIKE :keyword) ");
            params.put("keyword", like);
        }

        String fromClause = """
            FROM product_retail_access_projection prap
            JOIN product_passports pp ON pp.passport_id = prap.passport_id
            JOIN product_assets pa ON pa.asset_id = pp.asset_id
        """;

        Query countQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) " + fromClause + whereClause, Long.class);
        params.forEach(countQuery::setParameter);
        long total = (Long) countQuery.getSingleResult();
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);

        Query contentQuery = entityManager.createNativeQuery(
            """
                SELECT prap.passport_id,
                       pp.qr_public_code,
                       pa.asset_id,
                       pa.serial_number,
                       pa.model_id,
                       pa.model_name,
                       pa.asset_state,
                       pa.risk_flag,
                       prap.access_source_type,
                       prap.access_source_id,
                       prap.expires_at,
                       prap.source_tenant_id,
                       prap.tenant_id,
                       prap.access_status,
                       prap.granted_at
            """ + fromClause + whereClause + " ORDER BY prap.granted_at DESC ",
            Tuple.class);
        params.forEach(contentQuery::setParameter);
        contentQuery.setFirstResult(page * size);
        contentQuery.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Tuple> rows = contentQuery.getResultList();

        List<RetailAccessRow> content = rows.stream()
            .map(t -> new RetailAccessRow(
                t.get("passport_id", String.class),
                t.get("qr_public_code", String.class),
                t.get("asset_id", String.class),
                t.get("serial_number", String.class),
                t.get("model_id", String.class),
                t.get("model_name", String.class),
                t.get("asset_state", String.class),
                t.get("risk_flag", String.class),
                t.get("access_source_type", String.class),
                t.get("access_source_id", String.class),
                toInstant((Timestamp) t.get("expires_at")),
                t.get("source_tenant_id", String.class),
                t.get("tenant_id", String.class),
                t.get("access_status", String.class),
                ((Timestamp) t.get("granted_at")).toInstant()
            ))
            .toList();

        return new PagedRetailAccessResult(content, page, size, total, totalPages);
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
