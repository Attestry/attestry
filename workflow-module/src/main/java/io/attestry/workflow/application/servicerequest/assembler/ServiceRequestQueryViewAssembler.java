package io.attestry.workflow.application.servicerequest.assembler;

import io.attestry.workflow.application.port.servicerequest.ServiceProductReadPort;
import io.attestry.workflow.application.usecase.ServiceRequestQueryUseCase.PagedServiceRequestResult;
import io.attestry.workflow.application.usecase.ServiceRequestQueryUseCase.ServiceRequestListItemResult;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestQueryViewAssembler {

    private final ServiceRequestEvidenceAssembler evidenceAssembler;

    public ServiceRequestQueryViewAssembler(ServiceRequestEvidenceAssembler evidenceAssembler) {
        this.evidenceAssembler = evidenceAssembler;
    }

    public ServiceRequestListItemResult toListItem(
        ServiceRequest request,
        String providerTenantName,
        ServiceProductReadPort.ServicePassportAssetInfo passportAssetInfo
    ) {
        return new ServiceRequestListItemResult(
            request.serviceRequestId(),
            request.passportId(),
            passportAssetInfo.serialNumber(),
            passportAssetInfo.modelName(),
            request.providerTenantId(),
            providerTenantName,
            request.serviceType(),
            request.description(),
            request.serviceRequestMethod(),
            request.symptomDescription(),
            request.requestedReservationAt(),
            request.contactMemo(),
            request.beforeEvidenceGroupId(),
            evidenceAssembler.toEvidenceFiles(request.beforeEvidenceGroupId()),
            request.afterEvidenceGroupId(),
            evidenceAssembler.toEvidenceFiles(request.afterEvidenceGroupId()),
            request.serviceResultDetail(),
            request.completionMemo(),
            request.status() == ServiceRequestStatus.REJECTED ? request.cancelReason() : null,
            request.status() == ServiceRequestStatus.CANCELLED ? request.cancelReason() : null,
            request.status().name(),
            request.submittedAt(),
            request.completedAt()
        );
    }

    public PagedServiceRequestResult toPagedResult(
        List<ServiceRequestListItemResult> content,
        int page,
        int size,
        long totalElements
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PagedServiceRequestResult(content, page, size, totalElements, totalPages);
    }

    public ServiceProductReadPort.ServicePassportAssetInfo defaultAssetInfo(String passportId) {
        return new ServiceProductReadPort.ServicePassportAssetInfo(passportId, null, null);
    }
}
