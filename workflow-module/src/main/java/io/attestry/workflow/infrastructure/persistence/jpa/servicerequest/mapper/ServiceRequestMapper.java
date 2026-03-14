package io.attestry.workflow.infrastructure.persistence.jpa.servicerequest.mapper;

import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.infrastructure.persistence.jpa.servicerequest.entity.WorkflowServiceRequestJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestMapper {

    public ServiceRequest toDomain(WorkflowServiceRequestJpaEntity entity) {
        return new ServiceRequest(
            entity.getServiceRequestId(),
            entity.getPassportId(),
            entity.getServiceType(),
            entity.getOwnerUserId(),
            entity.getProviderTenantId(),
            entity.getStatus(),
            entity.getDescription(),
            entity.getServiceRequestMethod(),
            entity.getSymptomDescription(),
            entity.getRequestedReservationAt(),
            entity.getContactMemo(),
            entity.getBeforeEvidenceGroupId(),
            entity.getAfterEvidenceGroupId(),
            entity.getServiceResultDetail(),
            entity.getCompletionMemo(),
            entity.getPermissionId(),
            entity.getSubmittedByUserId(),
            entity.getSubmittedAt(),
            entity.getCompletedAt(),
            entity.getCompletedByUserId(),
            entity.getCancelledAt(),
            entity.getCancelReason(),
            entity.getCreatedAt()
        );
    }

    public WorkflowServiceRequestJpaEntity toEntity(ServiceRequest domain) {
        return new WorkflowServiceRequestJpaEntity(
            domain.serviceRequestId(),
            domain.passportId(),
            domain.serviceType(),
            domain.ownerUserId(),
            domain.providerTenantId(),
            domain.status(),
            domain.description(),
            domain.serviceRequestMethod(),
            domain.symptomDescription(),
            domain.requestedReservationAt(),
            domain.contactMemo(),
            domain.beforeEvidenceGroupId(),
            domain.afterEvidenceGroupId(),
            domain.serviceResultDetail(),
            domain.completionMemo(),
            domain.permissionId(),
            domain.submittedByUserId(),
            domain.submittedAt(),
            domain.completedAt(),
            domain.completedByUserId(),
            domain.cancelledAt(),
            domain.cancelReason(),
            domain.createdAt()
        );
    }
}
