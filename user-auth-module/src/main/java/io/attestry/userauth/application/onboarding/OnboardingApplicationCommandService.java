package io.attestry.userauth.application.onboarding;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.CreateApplicationCommand;
import io.attestry.userauth.application.dto.command.PresignEvidenceUploadCommand;
import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.dto.result.ApproveApplicationResult;
import io.attestry.userauth.application.dto.result.EvidenceBundleResult;
import io.attestry.userauth.application.dto.result.PresignedEvidenceUploadResult;
import io.attestry.userauth.application.onboarding.OnboardingProvisioningService.ProvisioningResult;
import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.userauth.application.port.OnboardingEvidenceBundleRepositoryPort;
import io.attestry.userauth.application.port.OrganizationApplicationRepositoryPort;
import io.attestry.userauth.application.usecase.onboarding.OnboardingApplicationCommandUseCase;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.onboarding.policy.OrganizationUniquenessPolicy;
import io.attestry.userauth.domain.tenant.model.TenantType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Service
public class OnboardingApplicationCommandService implements OnboardingApplicationCommandUseCase {

    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    private final OrganizationApplicationRepositoryPort applicationRepository;
    private final OnboardingEvidenceBundleRepositoryPort evidenceBundleRepository;
    private final OnboardingProvisioningService provisioningService;
    private final OrganizationUniquenessPolicy uniquenessPolicy;
    private final ObjectStoragePort objectStoragePort;
    private final OnboardingApplicationViewAssembler viewAssembler;
    private final OnboardingEvidenceUploadCompletionTxService onboardingEvidenceUploadCompletionTxService;
    private final Clock clock;

    @Override
    @Transactional
    public ApplicationResult createApplication(ActorContext actor, CreateApplicationCommand command) {
        TenantType type = TenantType.parseSupported(command.type());
        requireReadyEvidenceOwnedByPrincipal(actor.userId(), command.evidenceBundleId());

        OrganizationApplication application = switch (type) {
            case BRAND -> {
                uniquenessPolicy.assertUniqueBrand(command.orgName(), command.country(), command.bizRegNo());
                yield OrganizationApplication.createBrand(
                    actor.userId(),
                    command.orgName(),
                    command.country(),
                    command.bizRegNo(),
                    command.address(),
                    command.evidenceBundleId()
                );
            }
            case RETAIL -> {
                uniquenessPolicy.assertUniqueRetail(command.orgName(), command.country(), command.bizRegNo());
                yield OrganizationApplication.createRetail(
                    actor.userId(),
                    command.orgName(),
                    command.country(),
                    command.bizRegNo(),
                    command.address(),
                    command.evidenceBundleId()
                );
            }
            case SERVICE -> {
                uniquenessPolicy.assertUniqueService(command.orgName(), command.country(), command.bizRegNo());
                yield OrganizationApplication.createService(
                    actor.userId(),
                    command.orgName(),
                    command.country(),
                    command.bizRegNo(),
                    command.address(),
                    command.evidenceBundleId()
                );
            }
            default -> throw new UserAuthDomainException(
                UserAuthErrorCode.INVALID_REQUEST,
                "Only BRAND, RETAIL, or SERVICE application type is supported"
            );
        };

        return viewAssembler.toResult(applicationRepository.save(application));
    }

    @Override
    @Transactional
    public ApproveApplicationResult approveApplication(ActorContext actor, String applicationId) {
        OrganizationApplication app = findApplication(applicationId);
        app.assertPending();

        ProvisioningResult result = provisioningService.provision(
            app.type(), app.applicantUserId(), app.orgName(), app.country(), app.address(), actor.userId());

        app.approve(actor.userId(), result.tenantId(), Instant.now(clock));
        applicationRepository.save(app);

        return new ApproveApplicationResult(result.tenantId(), result.membershipId());
    }

    @Override
    @Transactional
    public ApplicationResult rejectApplication(ActorContext actor, String applicationId, String rejectReason) {
        OrganizationApplication app = findApplication(applicationId);
        app.reject(actor.userId(), rejectReason, Instant.now(clock));
        return viewAssembler.toResult(applicationRepository.save(app));
    }

    @Override
    @Transactional
    public PresignedEvidenceUploadResult presignEvidenceUpload(ActorContext actor, PresignEvidenceUploadCommand command) {
        Instant now = Instant.now(clock);
        OnboardingEvidenceBundle bundle = resolveOrCreateBundle(actor, command.evidenceBundleId(), now);
        String objectKey = buildObjectKey(actor.userId(), bundle.evidenceBundleId(), command.fileName());

        OnboardingEvidenceFile evidenceFile = bundle.addFile(
            command.fileName(),
            command.contentType(),
            objectKey,
            now
        );
        evidenceBundleRepository.save(bundle);

        ObjectStoragePort.PresignedUpload presignedUpload = objectStoragePort.issuePresignedUpload(
            objectKey,
            command.contentType(),
            PRESIGN_TTL
        );

        return new PresignedEvidenceUploadResult(
            bundle.evidenceBundleId(),
            evidenceFile.evidenceFileId(),
            objectKey,
            presignedUpload.uploadUrl(),
            presignedUpload.expiresAt()
        );
    }

    @Override
    public EvidenceBundleResult completeEvidenceUpload(ActorContext actor, CompleteEvidenceUploadCommand command) {
        OnboardingEvidenceFile evidenceFile = evidenceBundleRepository.findFileById(command.evidenceFileId())
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_NOT_FOUND, "Evidence file not found"));
        if (!objectStoragePort.objectExists(evidenceFile.objectKey())) {
            throw new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_NOT_FOUND, "Uploaded object not found");
        }

        return onboardingEvidenceUploadCompletionTxService.completeEvidenceUpload(actor, command);
    }

    private OrganizationApplication findApplication(String applicationId) {
        return applicationRepository.findById(applicationId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
    }

    private void requireReadyEvidenceOwnedByPrincipal(String userId, String evidenceBundleId) {
        requireText(evidenceBundleId, "evidenceBundleId");
        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(evidenceBundleId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
        bundle.assertOwnedBy(userId);
        bundle.assertReady();
    }

    private OnboardingEvidenceBundle resolveOrCreateBundle(ActorContext actor, String requestedBundleId, Instant now) {
        if (requestedBundleId == null || requestedBundleId.isBlank()) {
            return evidenceBundleRepository.save(OnboardingEvidenceBundle.create(actor.userId(), now));
        }

        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(requestedBundleId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
        bundle.assertOwnedBy(actor.userId());
        bundle.assertCollecting();
        return bundle;
    }

    private String buildObjectKey(String userId, String bundleId, String fileName) {
        String safeFileName = fileName == null ? "evidence.bin" : fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "onboarding/" + userId + "/" + bundleId + "/" + UUID.randomUUID() + "/" + safeFileName;
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_APPLICATION_STATE, fieldName + " is required");
        }
    }
}
