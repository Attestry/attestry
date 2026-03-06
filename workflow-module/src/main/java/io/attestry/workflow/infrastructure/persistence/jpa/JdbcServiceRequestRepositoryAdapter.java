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
                SET status = EXCLUDED.status,
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
    public boolean existsSubmittedByPassportId(String passportId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM workflow_service_requests WHERE passport_id = ? AND status = 'SUBMITTED'",
            Integer.class,
            passportId
        );
        return count != null && count > 0;
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
