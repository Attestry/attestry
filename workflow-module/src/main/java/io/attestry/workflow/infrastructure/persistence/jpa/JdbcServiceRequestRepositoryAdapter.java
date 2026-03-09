package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcServiceRequestRepositoryAdapter implements ServiceRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcServiceRequestRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ServiceRequest save(ServiceRequest request) {
        jdbcTemplate.update(
            """
                INSERT INTO workflow_service_requests (
                    service_request_id, passport_id, service_type,
                    owner_user_id, provider_tenant_id,
                    status, description,
                    before_evidence_group_id, after_evidence_group_id,
                    permission_id, submitted_by_user_id,
                    submitted_at, completed_at, completed_by_user_id,
                    cancelled_at, cancel_reason, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (service_request_id) DO UPDATE
                SET service_type = EXCLUDED.service_type,
                    description = EXCLUDED.description,
                    status = EXCLUDED.status,
                    after_evidence_group_id = EXCLUDED.after_evidence_group_id,
                    completed_at = EXCLUDED.completed_at,
                    completed_by_user_id = EXCLUDED.completed_by_user_id,
                    cancelled_at = EXCLUDED.cancelled_at,
                    cancel_reason = EXCLUDED.cancel_reason
            """,
            request.serviceRequestId(),
            request.passportId(),
            request.serviceType(),
            request.ownerUserId(),
            request.providerTenantId(),
            request.status().name(),
            request.description(),
            request.beforeEvidenceGroupId(),
            request.afterEvidenceGroupId(),
            request.permissionId(),
            request.submittedByUserId(),
            Timestamp.from(request.submittedAt()),
            request.completedAt() == null ? null : Timestamp.from(request.completedAt()),
            request.completedByUserId(),
            request.cancelledAt() == null ? null : Timestamp.from(request.cancelledAt()),
            request.cancelReason(),
            Timestamp.from(request.createdAt())
        );

        return jdbcTemplate.queryForObject(
            "SELECT * FROM workflow_service_requests WHERE service_request_id = ?",
            (rs, rowNum) -> mapServiceRequest(rs),
            request.serviceRequestId()
        );
    }

    @Override
    public Optional<ServiceRequest> findById(String serviceRequestId) {
        List<ServiceRequest> rows = jdbcTemplate.query(
            "SELECT * FROM workflow_service_requests WHERE service_request_id = ?",
            (rs, rowNum) -> mapServiceRequest(rs),
            serviceRequestId
        );
        return rows.stream().findFirst();
    }

    @Override
    public boolean existsOpenByPassportId(String passportId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(1) FROM workflow_service_requests
                WHERE passport_id = ?
                  AND status IN ('PENDING', 'ACCEPTED')
            """,
            Integer.class,
            passportId
        );
        return count != null && count > 0;
    }

    @Override
    public List<ServiceRequest> findByOwnerUserId(String ownerUserId, ServiceRequestStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int offset = safePage * safeSize;
        if (status == null) {
            return jdbcTemplate.query(
                """
                    SELECT * FROM workflow_service_requests
                    WHERE owner_user_id = ?
                    ORDER BY created_at DESC
                    LIMIT ? OFFSET ?
                """,
                (rs, rowNum) -> mapServiceRequest(rs),
                ownerUserId,
                safeSize,
                offset
            );
        }
        return jdbcTemplate.query(
            """
                SELECT * FROM workflow_service_requests
                WHERE owner_user_id = ?
                  AND status = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> mapServiceRequest(rs),
            ownerUserId,
            status.name(),
            safeSize,
            offset
        );
    }

    @Override
    public long countByOwnerUserId(String ownerUserId, ServiceRequestStatus status) {
        if (status == null) {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM workflow_service_requests WHERE owner_user_id = ?",
                Long.class,
                ownerUserId
            );
            return count == null ? 0L : count;
        }
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM workflow_service_requests WHERE owner_user_id = ? AND status = ?",
            Long.class,
            ownerUserId,
            status.name()
        );
        return count == null ? 0L : count;
    }

    @Override
    public List<ServiceRequest> findByProviderTenantId(String providerTenantId, ServiceRequestStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int offset = safePage * safeSize;
        if (status == null) {
            return jdbcTemplate.query(
                """
                    SELECT * FROM workflow_service_requests
                    WHERE provider_tenant_id = ?
                    ORDER BY created_at DESC
                    LIMIT ? OFFSET ?
                """,
                (rs, rowNum) -> mapServiceRequest(rs),
                providerTenantId,
                safeSize,
                offset
            );
        }
        return jdbcTemplate.query(
            """
                SELECT * FROM workflow_service_requests
                WHERE provider_tenant_id = ?
                  AND status = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> mapServiceRequest(rs),
            providerTenantId,
            status.name(),
            safeSize,
            offset
        );
    }

    @Override
    public long countByProviderTenantId(String providerTenantId, ServiceRequestStatus status) {
        if (status == null) {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM workflow_service_requests WHERE provider_tenant_id = ?",
                Long.class,
                providerTenantId
            );
            return count == null ? 0L : count;
        }
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM workflow_service_requests WHERE provider_tenant_id = ? AND status = ?",
            Long.class,
            providerTenantId,
            status.name()
        );
        return count == null ? 0L : count;
    }

    private ServiceRequest mapServiceRequest(ResultSet rs) throws SQLException {
        Timestamp completedAt = rs.getTimestamp("completed_at");
        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
        return new ServiceRequest(
            rs.getString("service_request_id"),
            rs.getString("passport_id"),
            rs.getString("service_type"),
            rs.getString("owner_user_id"),
            rs.getString("provider_tenant_id"),
            ServiceRequestStatus.valueOf(rs.getString("status")),
            rs.getString("description"),
            rs.getString("before_evidence_group_id"),
            rs.getString("after_evidence_group_id"),
            rs.getString("permission_id"),
            rs.getString("submitted_by_user_id"),
            rs.getTimestamp("submitted_at").toInstant(),
            completedAt == null ? null : completedAt.toInstant(),
            rs.getString("completed_by_user_id"),
            cancelledAt == null ? null : cancelledAt.toInstant(),
            rs.getString("cancel_reason"),
            rs.getTimestamp("created_at").toInstant()
        );
    }
}
