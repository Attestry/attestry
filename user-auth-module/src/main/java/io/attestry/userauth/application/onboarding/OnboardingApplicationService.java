package io.attestry.userauth.application.onboarding;

import io.attestry.userauth.application.dto.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.CreateApplicationCommand;
import io.attestry.userauth.application.dto.command.PresignEvidenceUploadCommand;
import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.dto.result.ApproveApplicationResult;
import io.attestry.userauth.application.dto.result.EvidenceBundleResult;
import io.attestry.userauth.application.dto.result.PresignedEvidenceUploadResult;
import io.attestry.userauth.application.dto.view.ApplicationView;
import io.attestry.userauth.application.onboarding.OnboardingProvisioningService.ProvisioningResult;
import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.application.port.OnboardingEvidenceBundleRepositoryPort;
import io.attestry.userauth.application.port.OrganizationApplicationRepositoryPort;
import io.attestry.userauth.application.usecase.onboarding.OnboardingUseCase;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.onboarding.policy.OrganizationUniquenessPolicy;
import io.attestry.userauth.domain.organization.model.TenantType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApplicationService implements OnboardingUseCase {

    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    private final OrganizationApplicationRepositoryPort applicationRepository;
    private final OnboardingEvidenceBundleRepositoryPort evidenceBundleRepository;
    private final OnboardingProvisioningService provisioningService;
    private final OrganizationUniquenessPolicy uniquenessPolicy;
    private final ObjectStoragePort objectStoragePort;
    private final Clock clock;

    public OnboardingApplicationService(
            OrganizationApplicationRepositoryPort applicationRepository,
            OnboardingEvidenceBundleRepositoryPort evidenceBundleRepository,
            OnboardingProvisioningService provisioningService,
            OrganizationUniquenessPolicy uniquenessPolicy,
            ObjectStoragePort objectStoragePort,
            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.evidenceBundleRepository = evidenceBundleRepository;
        this.provisioningService = provisioningService;
        this.uniquenessPolicy = uniquenessPolicy;
        this.objectStoragePort = objectStoragePort;
        this.clock = clock;
    }

    // TODO("OrganizationApplication 에 자세한 지역정보 제공")
    @Override
    @Transactional
    public ApplicationResult createApplication(ActorContext actor, CreateApplicationCommand command) {
        TenantType type = parseRequestedType(command.type());
        requireReadyEvidenceOwnedByPrincipal(actor.userId(), command.evidenceBundleId());
        OrganizationApplication application = switch (type) {
            case BRAND -> {
                uniquenessPolicy.assertUniqueBrand(command.orgName(), command.country(), command.bizRegNo());
                yield OrganizationApplication.createBrand(
                        actor.userId(),
                        command.orgName(),
                        command.country(),
                        command.address(),
                        command.bizRegNo(),
                        command.evidenceBundleId());
            }
            case RETAIL -> {
                uniquenessPolicy.assertUniqueRetail(command.orgName(), command.country(), command.bizRegNo());
                yield OrganizationApplication.createRetail(
                        actor.userId(),
                        command.orgName(),
                        command.country(),
                        command.address(),
                        command.bizRegNo(),
                        command.evidenceBundleId());
            }
            case SERVICE -> {
                requireText(command.address(), "address");
                uniquenessPolicy.assertUniqueService(command.orgName(), command.country(), command.bizRegNo());
                yield OrganizationApplication.createService(
                        actor.userId(),
                        command.orgName(),
                        command.country(),
                        command.address(),
                        command.bizRegNo(),
                        command.evidenceBundleId());
            }
            default -> throw new DomainException(ErrorCode.INVALID_REQUEST,
                    "Only BRAND, RETAIL, or SERVICE application type is supported");
        };
        return toApplicationResult(applicationRepository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationView> listApplications(String type) {
        TenantType parsedType = parseOptionalRequestedType(type);
        List<OrganizationApplication> applications = parsedType == null
                ? applicationRepository.findAll()
                : applicationRepository.findByType(parsedType);
        return applications.stream()
                .map(this::toApplicationView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationView> listMyApplications(ActorContext actor) {
        return applicationRepository.findByApplicantUserId(actor.userId()).stream()
                .map(this::toApplicationView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationView getMyApplication(ActorContext actor, String applicationId) {
        OrganizationApplication application = applicationRepository
                .findByIdAndApplicantUserId(applicationId, actor.userId())
                .orElseThrow(() -> new DomainException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
        return toApplicationView(application);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationView getApplication(String applicationId) {
        OrganizationApplication application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new DomainException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
        return toApplicationView(application);
    }

    // TODO("이메일 발송로직")
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
        return toApplicationResult(applicationRepository.save(app));
    }

    @Override
    @Transactional
    public PresignedEvidenceUploadResult presignEvidenceUpload(ActorContext actor,
            PresignEvidenceUploadCommand command) {
        Instant now = Instant.now(clock);
        OnboardingEvidenceBundle bundle = resolveOrCreateBundle(actor, command.evidenceBundleId(), now);
        String objectKey = buildObjectKey(actor.userId(), bundle.evidenceBundleId(), command.fileName());

        OnboardingEvidenceFile evidenceFile = bundle.addFile(
                command.fileName(),
                command.contentType(),
                objectKey,
                now);
        evidenceBundleRepository.save(bundle);

        ObjectStoragePort.PresignedUpload presignedUpload = objectStoragePort.issuePresignedUpload(
                objectKey,
                command.contentType(),
                PRESIGN_TTL);

        return new PresignedEvidenceUploadResult(
                bundle.evidenceBundleId(),
                evidenceFile.evidenceFileId(),
                objectKey,
                presignedUpload.uploadUrl(),
                presignedUpload.expiresAt());
    }

    @Override
    @Transactional
    public EvidenceBundleResult completeEvidenceUpload(ActorContext actor, CompleteEvidenceUploadCommand command) {
        requireText(command.evidenceBundleId(), "evidenceBundleId");
        requireText(command.evidenceFileId(), "evidenceFileId");
        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(command.evidenceBundleId())
                .orElseThrow(() -> new DomainException(ErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
        bundle.assertOwnedBy(actor.userId());

        OnboardingEvidenceFile evidenceFile = bundle.files().stream()
                .filter(f -> f.evidenceFileId().equals(command.evidenceFileId()))
                .findFirst()
                .orElseThrow(
                        () -> new DomainException(ErrorCode.EVIDENCE_NOT_FOUND, "Evidence file not found in bundle"));

        if (!objectStoragePort.objectExists(evidenceFile.objectKey())) {
            throw new DomainException(ErrorCode.EVIDENCE_NOT_FOUND, "Uploaded object not found");
        }

        bundle.completeFile(command.evidenceFileId(), command.sizeBytes(), Instant.now(clock));
        evidenceBundleRepository.save(bundle);

        return new EvidenceBundleResult(bundle.evidenceBundleId(), bundle.status().name());
    }

    private OrganizationApplication findApplication(String applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new DomainException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
    }

    private void requireReadyEvidenceOwnedByPrincipal(String userId, String evidenceBundleId) {
        requireText(evidenceBundleId, "evidenceBundleId");
        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(evidenceBundleId)
                .orElseThrow(() -> new DomainException(ErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
        bundle.assertOwnedBy(userId);
        bundle.assertReady();
    }

    private ApplicationResult toApplicationResult(OrganizationApplication app) {
        return new ApplicationResult(
                app.applicationId(),
                app.type().name(),
                app.applicantUserId(),
                app.tenantId(),
                app.orgName(),
                app.country(),
                app.address(),
                app.bizRegNo(),
                app.evidenceBundleId(),
                app.status().name(),
                app.rejectReason());
    }

    private ApplicationView toApplicationView(OrganizationApplication app) {
        List<ApplicationView.EvidenceFileView> evidenceFiles = resolveEvidenceFiles(app.evidenceBundleId());
        return new ApplicationView(
                app.applicationId(),
                app.type().name(),
                app.applicantUserId(),
                app.tenantId(),
                app.orgName(),
                app.country(),
                app.address(),
                app.bizRegNo(),
                app.evidenceBundleId(),
                evidenceFiles,
                app.status().name(),
                app.rejectReason());
    }

    private List<ApplicationView.EvidenceFileView> resolveEvidenceFiles(String evidenceBundleId) {
        if (evidenceBundleId == null || evidenceBundleId.isBlank()) {
            return List.of();
        }

        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(evidenceBundleId).orElse(null);
        if (bundle == null || bundle.files().isEmpty()) {
            return List.of();
        }

        return bundle.files().stream()
                .filter(OnboardingEvidenceFile::isReady)
                .map(file -> {
                    String downloadUrl = null;
                    try {
                        ObjectStoragePort.PresignedDownload download =
                                objectStoragePort.issuePresignedDownload(file.objectKey(), PRESIGN_TTL);
                        downloadUrl = download.downloadUrl();
                    } catch (Exception ignored) {
                    }
                    return new ApplicationView.EvidenceFileView(
                            file.evidenceFileId(),
                            file.originalFileName(),
                            file.contentType(),
                            file.sizeBytes() != null ? file.sizeBytes() : 0,
                            downloadUrl
                    );
                })
                .toList();
    }

    private OnboardingEvidenceBundle resolveOrCreateBundle(ActorContext actor, String requestedBundleId, Instant now) {
        if (requestedBundleId == null || requestedBundleId.isBlank()) {
            return evidenceBundleRepository.save(OnboardingEvidenceBundle.create(actor.userId(), now));
        }
        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(requestedBundleId)
                .orElseThrow(() -> new DomainException(ErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
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
            throw new DomainException(ErrorCode.INVALID_APPLICATION_STATE, fieldName + " is required");
        }
    }

    private TenantType parseRequestedType(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "type is required");
        }
        try {
            TenantType type = TenantType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (type == TenantType.BRAND || type == TenantType.RETAIL || type == TenantType.SERVICE) {
                return type;
            }
        } catch (IllegalArgumentException ignored) {
            // handled below
        }
        throw new DomainException(ErrorCode.INVALID_REQUEST, "type must be BRAND, RETAIL, or SERVICE");
    }

    private TenantType parseOptionalRequestedType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseRequestedType(value);
    }
}
