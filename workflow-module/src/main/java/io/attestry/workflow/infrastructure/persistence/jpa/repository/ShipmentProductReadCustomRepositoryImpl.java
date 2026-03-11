package io.attestry.workflow.infrastructure.persistence.jpa.repository;

import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort.PagedReleaseCandidateResult;
import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort.PagedShipmentReadResult;
import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort.ShipmentJoinedRow;
import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort.ShipmentReleaseCandidate;
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
public class ShipmentProductReadCustomRepositoryImpl implements ShipmentProductReadCustomRepository {

    private final EntityManager entityManager;

    @Override
    public PagedReleaseCandidateResult findReleaseCandidatesWithFilters(
        String tenantId, int page, int size, String keyword
    ) {
        StringBuilder whereClause = new StringBuilder("""
            WHERE wpsp.tenant_id = :tenantId
              AND wpsp.asset_state = 'ACTIVE'
              AND wpsp.risk_flag = 'NONE'
              AND NOT EXISTS (
                   SELECT 1 FROM workflow_shipments ws
                   WHERE ws.passport_id = wpsp.passport_id AND ws.status = 'RELEASED'
              )
            """);
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId", tenantId);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" AND (LOWER(wpcp.serial_number) LIKE :keyword OR LOWER(wpcp.model_name) LIKE :keyword) ");
            params.put("keyword", like);
        }

        String fromClause = """
            FROM workflow_passport_state_projection wpsp
            JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = wpsp.passport_id
            """;

        Query countQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) " + fromClause + whereClause, Long.class);
        params.forEach(countQuery::setParameter);
        long total = (Long) countQuery.getSingleResult();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        Query contentQuery = entityManager.createNativeQuery(
            """
                SELECT wpcp.passport_id,
                       wpcp.asset_id,
                       wpcp.serial_number,
                       wpcp.model_id,
                       wpcp.model_name,
                       wpcp.production_batch,
                       wpcp.factory_code
            """ + fromClause + whereClause + " ORDER BY wpcp.updated_at DESC, wpcp.passport_id DESC ",
            Tuple.class);
        params.forEach(contentQuery::setParameter);
        contentQuery.setFirstResult(page * size);
        contentQuery.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Tuple> rows = contentQuery.getResultList();
        List<ShipmentReleaseCandidate> content = rows.stream()
            .map(t -> new ShipmentReleaseCandidate(
                t.get("passport_id", String.class),
                t.get("asset_id", String.class),
                t.get("serial_number", String.class),
                t.get("model_id", String.class),
                t.get("model_name", String.class),
                t.get("production_batch", String.class),
                t.get("factory_code", String.class)
            ))
            .toList();

        return new PagedReleaseCandidateResult(content, page, size, total, totalPages);
    }

    @Override
    public PagedShipmentReadResult findShipmentsWithFilters(
        String tenantId, int page, int size, String keyword
    ) {
        StringBuilder whereClause = new StringBuilder(" WHERE ws.tenant_id = :tenantId ");
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId", tenantId);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" AND (LOWER(wpcp.serial_number) LIKE :keyword OR LOWER(wpcp.model_name) LIKE :keyword) ");
            params.put("keyword", like);
        }

        String fromClause = """
            FROM workflow_shipments ws
            JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = ws.passport_id
            """;

        Query countQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) " + fromClause + whereClause, Long.class);
        params.forEach(countQuery::setParameter);
        long total = (Long) countQuery.getSingleResult();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        Query contentQuery = entityManager.createNativeQuery(
            """
                SELECT ws.shipment_id, ws.tenant_id, ws.passport_id,
                       wpcp.asset_id, wpcp.serial_number, wpcp.model_id, wpcp.model_name,
                       wpcp.production_batch, wpcp.factory_code,
                       ws.shipment_round, ws.status,
                       ws.released_at, ws.released_by_user_id, ws.released_by_tenant_id,
                       ws.evidence_group_id,
                       ws.returned_at, ws.returned_by_user_id, ws.return_evidence_group_id,
                       ws.created_at
            """ + fromClause + whereClause + " ORDER BY ws.created_at DESC ",
            Tuple.class);
        params.forEach(contentQuery::setParameter);
        contentQuery.setFirstResult(page * size);
        contentQuery.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Tuple> rows = contentQuery.getResultList();
        List<ShipmentJoinedRow> content = rows.stream()
            .map(t -> new ShipmentJoinedRow(
                t.get("shipment_id", String.class),
                t.get("tenant_id", String.class),
                t.get("passport_id", String.class),
                t.get("asset_id", String.class),
                t.get("serial_number", String.class),
                t.get("model_id", String.class),
                t.get("model_name", String.class),
                t.get("production_batch", String.class),
                t.get("factory_code", String.class),
                t.get("shipment_round", Integer.class),
                t.get("status", String.class),
                toInstant((Timestamp) t.get("released_at")),
                t.get("released_by_user_id", String.class),
                t.get("released_by_tenant_id", String.class),
                t.get("evidence_group_id", String.class),
                toInstant((Timestamp) t.get("returned_at")),
                t.get("returned_by_user_id", String.class),
                t.get("return_evidence_group_id", String.class),
                ((Timestamp) t.get("created_at")).toInstant()
            ))
            .toList();

        return new PagedShipmentReadResult(content, page, size, total, totalPages);
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
