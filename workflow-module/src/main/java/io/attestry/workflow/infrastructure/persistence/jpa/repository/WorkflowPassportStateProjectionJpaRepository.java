package io.attestry.workflow.infrastructure.persistence.jpa.repository;

import io.attestry.workflow.infrastructure.persistence.jpa.entity.WorkflowPassportStateProjectionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowPassportStateProjectionJpaRepository
    extends JpaRepository<WorkflowPassportStateProjectionEntity, String> {

    @Query(value = """
        SELECT passport_id AS passportId,
               tenant_id AS tenantId,
               asset_state AS assetState,
               risk_flag AS riskFlag
        FROM workflow_passport_state_projection
        WHERE passport_id = :passportId
        """, nativeQuery = true)
    Optional<PassportStateProjection> findPassportStateById(@Param("passportId") String passportId);

    interface PassportStateProjection {
        String getPassportId();
        String getTenantId();
        String getAssetState();
        String getRiskFlag();
    }
}
