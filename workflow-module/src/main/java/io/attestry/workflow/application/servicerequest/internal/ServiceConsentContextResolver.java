package io.attestry.workflow.application.servicerequest.internal;

import io.attestry.workflow.application.port.servicerequest.ServiceProductReadPort;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy;
import org.springframework.stereotype.Component;

@Component
public class ServiceConsentContextResolver {

    private final ServiceProductReadPort serviceProductReadPort;

    public ServiceConsentContextResolver(ServiceProductReadPort serviceProductReadPort) {
        this.serviceProductReadPort = serviceProductReadPort;
    }

    public ServiceConsentPolicy.ServiceConsentContext resolveGrantContext(String principalUserId, String passportId) {
        ServiceProductReadPort.ServicePassportState state = serviceProductReadPort.findPassportState(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));

        String currentOwnerId = serviceProductReadPort.findCurrentOwnerId(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport owner not found"));

        return new ServiceConsentPolicy.ServiceConsentContext(
            principalUserId,
            currentOwnerId,
            state.assetState(),
            state.riskFlag()
        );
    }

    public String resolveCurrentOwnerId(String passportId) {
        return serviceProductReadPort.findCurrentOwnerId(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport owner not found"));
    }
}
