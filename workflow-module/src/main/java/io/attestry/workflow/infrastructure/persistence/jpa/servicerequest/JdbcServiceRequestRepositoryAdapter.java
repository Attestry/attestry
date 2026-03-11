package io.attestry.workflow.infrastructure.persistence.jpa.servicerequest;

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

    private static final String OPEN_STATUS_PENDING = ServiceRequestStatus.PENDING.name();
    private static final String OPEN_STATUS_ACCEPTED = ServiceRequestStatus.ACCEPTED.name();

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
                    status, description, service_request_method, symptom_description, requested_reservation_at, contact_memo,
                    before_evidence_group_id, after_evidence_group_id,
                    service_result_detail, completion_memo,
                    permission_id, submitted_by_user_id,
                    submitted_at, completed_at, completed_by_user_id,
                    cancelled_at, cancel_reason, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (service_request_id) DO UPDATE
                SET service_type = EXCLUDED.service_type,
                    description = EXCLUDED.description,
                    service_request_method = EXCLUDED.service_request_method,
                    symptom_description = EXCLUDED.symptom_description,
                    requested_reservation_at = EXCLUDED.requested_reservation_at,
                    contact_memo = EXCLUDED.contact_memo,
                    status = EXCLUDED.status,
                    after_evidence_group_id = EXCLUDED.after_evidence_group_id,
                    service_result_detail = EXCLUDED.service_result_detail,
                    completion_memo = EXCLUDED.completion_memo,
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
            request.serviceRequestMethod(),
            request.symptomDescription(),
            request.requestedReservationAt() == null ? null : Timestamp.from(request.requestedReservationAt()),
            request.contactMemo(),
            request.beforeEvidenceGroupId(),
            request.afterEvidenceGroupId(),
            request.serviceResultDetail(),
            request.completionMemo(),
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
                  AND status IN (?, ?)
            """,
            Integer.class,
            passportId,
            OPEN_STATUS_PENDING,
            OPEN_STATUS_ACCEPTED
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
            rs.getString("service_request_method"),
            rs.getString("symptom_description"),
            rs.getTimestamp("requested_reservation_at") == null ? null : rs.getTimestamp("requested_reservation_at").toInstant(),
            rs.getString("contact_memo"),
            rs.getString("before_evidence_group_id"),
            rs.getString("after_evidence_group_id"),
            rs.getString("service_result_detail"),
            rs.getString("completion_memo"),
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
