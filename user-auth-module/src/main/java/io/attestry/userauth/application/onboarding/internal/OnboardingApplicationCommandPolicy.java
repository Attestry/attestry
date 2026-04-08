package io.attestry.userauth.application.onboarding.internal;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.port.onboarding.OnboardingEvidenceBundleRepositoryPort;
import io.attestry.userauth.application.port.onboarding.OrganizationApplicationRepositoryPort;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingApplicationCommandPolicy {

    private final OrganizationApplicationRepositoryPort applicationRepository;
    private final OnboardingEvidenceBundleRepositoryPort evidenceBundleRepository;

    public OrganizationApplication findApplication(String applicationId) {
        return applicationRepository.findById(applicationId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
    }

    public void requireReadyEvidenceOwnedByPrincipal(String userId, String evidenceBundleId) {
        requireText(evidenceBundleId, "evidenceBundleId");
        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(evidenceBundleId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
        bundle.assertOwnedBy(userId);
        bundle.assertReady();
    }

    public OnboardingEvidenceBundle resolveOrCreateBundle(ActorContext actor, String requestedBundleId, Instant now) {
        if (requestedBundleId == null || requestedBundleId.isBlank()) {
            return evidenceBundleRepository.save(OnboardingEvidenceBundle.create(actor.userId(), now));
        }

        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(requestedBundleId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
        bundle.assertOwnedBy(actor.userId());
        bundle.assertCollecting();
        return bundle;
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_APPLICATION_STATE, fieldName + " is required");
        }
    }
}
