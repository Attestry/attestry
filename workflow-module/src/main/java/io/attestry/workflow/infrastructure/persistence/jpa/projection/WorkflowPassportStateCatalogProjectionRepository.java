package io.attestry.workflow.infrastructure.persistence.jpa.projection;

import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort.ProductStatePayload;
import java.sql.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class WorkflowPassportStateCatalogProjectionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    void upsertStateAndCatalog(
        ProductStatePayload payload,
        String sourceEventId,
        Long sourceEventVersion,
        Timestamp updatedAt
    ) {
        upsertStateProjection(payload, sourceEventId, sourceEventVersion, updatedAt);
        upsertCatalogProjection(payload, sourceEventId, sourceEventVersion, updatedAt);
    }

    private void upsertStateProjection(
        ProductStatePayload payload,
        String sourceEventId,
        Long sourceEventVersion,
        Timestamp updatedAt
    ) {
        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO workflow_passport_state_projection (
                    passport_id,
                    tenant_id,
                    asset_id,
                    asset_state,
                    risk_flag,
                    current_owner_id,
                    source_event_id,
                    source_event_version,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?)
                ON CONFLICT (passport_id) DO UPDATE SET
                    tenant_id = EXCLUDED.tenant_id,
                    asset_id = EXCLUDED.asset_id,
                    asset_state = EXCLUDED.asset_state,
                    risk_flag = EXCLUDED.risk_flag,
                    current_owner_id = COALESCE(
                        workflow_passport_state_projection.current_owner_id,
                        EXCLUDED.current_owner_id
                    ),
                    source_event_id = EXCLUDED.source_event_id,
                    source_event_version = EXCLUDED.source_event_version,
                    updated_at = EXCLUDED.updated_at
            """,
            payload.passportId(),
            payload.tenantId(),
            payload.assetId(),
            payload.assetState(),
            payload.riskFlag(),
            sourceEventId,
            sourceEventVersion,
            updatedAt
        );
    }

    private void upsertCatalogProjection(
        ProductStatePayload payload,
        String sourceEventId,
        Long sourceEventVersion,
        Timestamp updatedAt
    ) {
        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO workflow_passport_catalog_projection (
                    passport_id,
                    asset_id,
                    serial_number,
                    model_id,
                    model_name,
                    production_batch,
                    factory_code,
                    manufactured_at,
                    source_event_id,
                    source_event_version,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::timestamp, ?, ?, ?)
                ON CONFLICT (passport_id) DO UPDATE SET
                    asset_id = EXCLUDED.asset_id,
                    serial_number = EXCLUDED.serial_number,
                    model_id = EXCLUDED.model_id,
                    model_name = EXCLUDED.model_name,
                    production_batch = EXCLUDED.production_batch,
                    factory_code = EXCLUDED.factory_code,
                    manufactured_at = EXCLUDED.manufactured_at,
                    source_event_id = EXCLUDED.source_event_id,
                    source_event_version = EXCLUDED.source_event_version,
                    updated_at = EXCLUDED.updated_at
            """,
            payload.passportId(),
            payload.assetId(),
            payload.serialNumber(),
            payload.modelId(),
            payload.modelName(),
            payload.productionBatch(),
            payload.factoryCode(),
            payload.manufacturedAt(),
            sourceEventId,
            sourceEventVersion,
            updatedAt
        );
    }
}
