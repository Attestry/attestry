package io.attestry.workflow.infrastructure.persistence.jpa.servicerequest.entity;

import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "workflow_service_requests")
public class WorkflowServiceRequestJpaEntity {

    @Id
    @Column(name = "service_request_id", nullable = false, length = 36)
    private String serviceRequestId;

    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Column(name = "service_type")
    private String serviceType;

    @Column(name = "owner_user_id", nullable = false, length = 36)
    private String ownerUserId;

    @Column(name = "provider_tenant_id", nullable = false, length = 36)
    private String providerTenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ServiceRequestStatus status;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "service_request_method", length = 30)
    private String serviceRequestMethod;

    @Column(name = "symptom_description", length = 2000)
    private String symptomDescription;

    @Column(name = "requested_reservation_at")
    private Instant requestedReservationAt;

    @Column(name = "contact_memo", length = 2000)
    private String contactMemo;

    @Column(name = "before_evidence_group_id", length = 36)
    private String beforeEvidenceGroupId;

    @Column(name = "after_evidence_group_id", length = 36)
    private String afterEvidenceGroupId;

    @Column(name = "service_result_detail", length = 2000)
    private String serviceResultDetail;

    @Column(name = "completion_memo", length = 2000)
    private String completionMemo;

    @Column(name = "permission_id", length = 36)
    private String permissionId;

    @Column(name = "submitted_by_user_id", nullable = false, length = 36)
    private String submittedByUserId;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by_user_id", length = 36)
    private String completedByUserId;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 2000)
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkflowServiceRequestJpaEntity() {
    }

    public WorkflowServiceRequestJpaEntity(
        String serviceRequestId,
        String passportId,
        String serviceType,
        String ownerUserId,
        String providerTenantId,
        ServiceRequestStatus status,
        String description,
        String serviceRequestMethod,
        String symptomDescription,
        Instant requestedReservationAt,
        String contactMemo,
        String beforeEvidenceGroupId,
        String afterEvidenceGroupId,
        String serviceResultDetail,
        String completionMemo,
        String permissionId,
        String submittedByUserId,
        Instant submittedAt,
        Instant completedAt,
        String completedByUserId,
        Instant cancelledAt,
        String cancelReason,
        Instant createdAt
    ) {
        this.serviceRequestId = serviceRequestId;
        this.passportId = passportId;
        this.serviceType = serviceType;
        this.ownerUserId = ownerUserId;
        this.providerTenantId = providerTenantId;
        this.status = status;
        this.description = description;
        this.serviceRequestMethod = serviceRequestMethod;
        this.symptomDescription = symptomDescription;
        this.requestedReservationAt = requestedReservationAt;
        this.contactMemo = contactMemo;
        this.beforeEvidenceGroupId = beforeEvidenceGroupId;
        this.afterEvidenceGroupId = afterEvidenceGroupId;
        this.serviceResultDetail = serviceResultDetail;
        this.completionMemo = completionMemo;
        this.permissionId = permissionId;
        this.submittedByUserId = submittedByUserId;
        this.submittedAt = submittedAt;
        this.completedAt = completedAt;
        this.completedByUserId = completedByUserId;
        this.cancelledAt = cancelledAt;
        this.cancelReason = cancelReason;
        this.createdAt = createdAt;
    }

    public String getServiceRequestId() { return serviceRequestId; }
    public String getPassportId() { return passportId; }
    public String getServiceType() { return serviceType; }
    public String getOwnerUserId() { return ownerUserId; }
    public String getProviderTenantId() { return providerTenantId; }
    public ServiceRequestStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public String getServiceRequestMethod() { return serviceRequestMethod; }
    public String getSymptomDescription() { return symptomDescription; }
    public Instant getRequestedReservationAt() { return requestedReservationAt; }
    public String getContactMemo() { return contactMemo; }
    public String getBeforeEvidenceGroupId() { return beforeEvidenceGroupId; }
    public String getAfterEvidenceGroupId() { return afterEvidenceGroupId; }
    public String getServiceResultDetail() { return serviceResultDetail; }
    public String getCompletionMemo() { return completionMemo; }
    public String getPermissionId() { return permissionId; }
    public String getSubmittedByUserId() { return submittedByUserId; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getCompletedByUserId() { return completedByUserId; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public Instant getCreatedAt() { return createdAt; }
}
