package io.attestry.workflow.application.distribution.internal;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.distribution.model.Distribution;
import io.attestry.workflow.domain.distribution.repository.DistributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DistributionLookupService {

    private final DistributionRepository distributionRepository;

    public Distribution getById(String distributionId) {
        return distributionRepository.findById(distributionId)
            .orElseThrow(() -> new WorkflowDomainException(
                WorkflowErrorCode.DISTRIBUTION_NOT_FOUND,
                "Distribution not found: " + distributionId
            ));
    }
}
