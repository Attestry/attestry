package io.attestry.userauth.application.onboarding;

import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.dto.view.ApplicationView;
import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.userauth.application.port.OnboardingEvidenceBundleRepositoryPort;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OnboardingApplicationViewAssembler {

    private static final Logger log = LoggerFactory.getLogger(OnboardingApplicationViewAssembler.class);
    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    private final OnboardingEvidenceBundleRepositoryPort evidenceBundleRepository;
    private final ObjectStoragePort objectStoragePort;


    public ApplicationResult toResult(OrganizationApplication app) {
        return new ApplicationResult(
            app.applicationId(),
            app.type().name(),
            app.applicantUserId(),
            app.tenantId(),
            app.orgName(),
            app.country(),
            app.bizRegNo(),
            app.evidenceBundleId(),
            app.status().name(),
            app.rejectReason()
        );
    }

    public ApplicationView toView(OrganizationApplication app) {
        OnboardingEvidenceBundle bundle = resolveBundle(app.evidenceBundleId());
        return toView(app, bundle);
    }

    public List<ApplicationView> toViews(List<OrganizationApplication> applications) {
        List<String> bundleIds = applications.stream()
            .map(OrganizationApplication::evidenceBundleId)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();

        Map<String, OnboardingEvidenceBundle> bundlesById = evidenceBundleRepository.findByIds(bundleIds).stream()
            .collect(java.util.stream.Collectors.toMap(
                OnboardingEvidenceBundle::evidenceBundleId,
                bundle -> bundle,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        return applications.stream()
            .map(app -> toView(app, bundlesById.get(app.evidenceBundleId())))
            .toList();
    }

    private ApplicationView toView(OrganizationApplication app, OnboardingEvidenceBundle bundle) {
        List<ApplicationView.EvidenceFileView> evidenceFiles = resolveEvidenceFiles(bundle);
        return new ApplicationView(
            app.applicationId(),
            app.type().name(),
            app.applicantUserId(),
            app.tenantId(),
            app.orgName(),
            app.country(),
            app.bizRegNo(),
            app.evidenceBundleId(),
            evidenceFiles,
            app.status().name(),
            app.rejectReason()
        );
    }

    private OnboardingEvidenceBundle resolveBundle(String evidenceBundleId) {
        if (evidenceBundleId == null || evidenceBundleId.isBlank()) {
            return null;
        }
        return evidenceBundleRepository.findById(evidenceBundleId).orElse(null);
    }

    private List<ApplicationView.EvidenceFileView> resolveEvidenceFiles(OnboardingEvidenceBundle bundle) {
        if (bundle == null || bundle.files().isEmpty()) {
            return List.of();
        }

        return bundle.files().stream()
            .filter(OnboardingEvidenceFile::isReady)
            .map(file -> new ApplicationView.EvidenceFileView(
                file.evidenceFileId(),
                file.originalFileName(),
                file.contentType(),
                file.sizeBytes() != null ? file.sizeBytes() : 0,
                issueDownloadUrl(file)
            ))
            .toList();
    }

    private String issueDownloadUrl(OnboardingEvidenceFile file) {
        try {
            ObjectStoragePort.PresignedDownload download =
                objectStoragePort.issuePresignedDownload(file.objectKey(), PRESIGN_TTL);
            return download.downloadUrl();
        } catch (Exception ex) {
            log.warn("Failed to issue evidence download URL. evidenceFileId={}, objectKey={}",
                file.evidenceFileId(), file.objectKey(), ex);
            return null;
        }
    }
}
