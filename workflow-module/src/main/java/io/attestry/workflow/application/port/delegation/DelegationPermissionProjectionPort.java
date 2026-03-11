package io.attestry.workflow.application.port.delegation;

import io.attestry.workflow.domain.delegation.model.Delegation;

public interface DelegationPermissionProjectionPort {

    void onDelegationGranted(Delegation delegation, String partnerLinkStatus);

    void onDelegationRevoked(Delegation delegation);

    void onDelegationConsumed(Delegation delegation);
}
