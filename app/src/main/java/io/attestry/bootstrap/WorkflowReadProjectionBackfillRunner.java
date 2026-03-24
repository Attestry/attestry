package io.attestry.bootstrap;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Backfill runner는 app 모듈 전용 bootstrap 코드로, 의도적으로 cross-module 테이블을 직접 쿼리합니다.
// 런타임 projection 동기화는 이벤트 payload 기반 (WorkflowProductProjectionConsumer)으로 동작하며,
// 이 runner는 전체 projection 재생성이 필요할 때만 enabled=true로 실행합니다.
@Component
@RequiredArgsConstructor
public class WorkflowReadProjectionBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowReadProjectionBackfillRunner.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ProjectionRunnerProperties runnerProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!runnerProperties.getWorkflow().getReadProjection().getBackfill().isEnabled()) {
            return;
        }

        backfillPassportStateProjection();
        backfillPassportCatalogProjection();
        backfillPassportPermissionProjection();
        backfillPassportOwnershipProjection();

        log.info("Workflow read projection backfill completed.");
    }

    private void backfillPassportStateProjection() {
        jdbcTemplate.getJdbcOperations().update("DELETE FROM workflow_passport_state_projection");
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
            )
            SELECT pp.passport_id,
                   pp.tenant_id,
                   pp.asset_id,
                   pa.asset_state,
                   CASE
                       WHEN pa.risk_flag = 'NONE' THEN 'NONE'
                       ELSE 'FLAGGED'
                   END,
                   po.owner_id,
                   'backfill:workflow-passport-state:' || pp.passport_id,
                   NULL,
                   CURRENT_TIMESTAMP
            FROM product_passports pp
            JOIN product_assets pa ON pa.asset_id = pp.asset_id
            LEFT JOIN passport_ownership po ON po.passport_id = pp.passport_id
            """
        );
    }

    private void backfillPassportCatalogProjection() {
        jdbcTemplate.getJdbcOperations().update("DELETE FROM workflow_passport_catalog_projection");
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
            )
            SELECT pp.passport_id,
                   pp.asset_id,
                   pa.serial_number,
                   pa.model_id,
                   pa.model_name,
                   pa.production_batch,
                   pa.factory_code,
                   pa.manufactured_at,
                   'backfill:workflow-passport-catalog:' || pp.passport_id,
                   NULL,
                   CURRENT_TIMESTAMP
            FROM product_passports pp
            JOIN product_assets pa ON pa.asset_id = pp.asset_id
            """
        );
    }

    private void backfillPassportPermissionProjection() {
        jdbcTemplate.getJdbcOperations().update("DELETE FROM workflow_passport_permission_projection");
        jdbcTemplate.getJdbcOperations().update(
            """
            INSERT INTO workflow_passport_permission_projection (
                permission_id,
                passport_id,
                seller_tenant_id,
                status,
                expires_at,
                source_event_id,
                source_event_version,
                updated_at
            )
            SELECT pp.permission_id,
                   pp.passport_id,
                   pp.seller_tenant_id,
                   pp.status,
                   pp.expires_at,
                   'backfill:workflow-passport-permission:' || pp.permission_id,
                   NULL,
                   CURRENT_TIMESTAMP
            FROM passport_permissions pp
            WHERE pp.seller_tenant_id IS NOT NULL
            """
        );
    }

    private void backfillPassportOwnershipProjection() {
        jdbcTemplate.getJdbcOperations().update("DELETE FROM workflow_passport_ownership_projection");
        jdbcTemplate.getJdbcOperations().update(
            """
            INSERT INTO workflow_passport_ownership_projection (
                passport_id,
                owner_id,
                source_event_id,
                source_event_version,
                updated_at
            )
            SELECT po.passport_id,
                   po.owner_id,
                   'backfill:workflow-passport-ownership:' || po.passport_id,
                   NULL,
                   CURRENT_TIMESTAMP
            FROM passport_ownership po
            WHERE po.owner_id IS NOT NULL
            """
        );
    }
}
