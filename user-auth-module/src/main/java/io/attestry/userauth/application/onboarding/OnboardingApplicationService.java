package io.attestry.userauth.application.onboarding;

import io.attestry.userauth.application.dto.command.CompleteEvidenceUploadCommand;
import io.attestry.userauth.application.dto.command.CreateBrandApplicationCommand;
import io.attestry.userauth.application.dto.command.CreateRetailApplicationCommand;
import io.attestry.userauth.application.dto.command.PresignEvidenceUploadCommand;
import io.attestry.userauth.application.dto.result.ApplicationResult;
import io.attestry.userauth.application.dto.result.ApproveApplicationResult;
import io.attestry.userauth.application.dto.result.EvidenceBundleResult;
import io.attestry.userauth.application.dto.result.PresignedEvidenceUploadResult;
import io.attestry.userauth.application.dto.view.ApplicationView;
import io.attestry.userauth.application.port.GroupRepositoryPort;
import io.attestry.userauth.application.port.MembershipProvisioningRepositoryPort;
import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.application.port.OnboardingEvidenceBundleRepositoryPort;
import io.attestry.userauth.application.port.OnboardingEvidenceFileRepositoryPort;
import io.attestry.userauth.application.port.OrganizationApplicationRepositoryPort;
import io.attestry.userauth.application.port.TenantRepositoryPort;
import io.attestry.userauth.application.usecase.onboarding.OnboardingUseCase;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.organization.model.Group;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.organization.model.Tenant;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import io.attestry.userauth.domain.policy.TenantIsolationPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApplicationService implements OnboardingUseCase {

    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);
    private static final int MAX_EVIDENCE_FILES_PER_BUNDLE = 5;

    private final OrganizationApplicationRepositoryPort applicationRepository;
    private final OnboardingEvidenceBundleRepositoryPort evidenceBundleRepository;
    private final OnboardingEvidenceFileRepositoryPort evidenceFileRepository;
    private final TenantRepositoryPort tenantRepository;
    private final GroupRepositoryPort groupRepository;
    private final MembershipProvisioningRepositoryPort membershipProvisioningRepository;
    private final ObjectStoragePort objectStoragePort;
    private final Clock clock;

    public OnboardingApplicationService(
        OrganizationApplicationRepositoryPort applicationRepository,
        OnboardingEvidenceBundleRepositoryPort evidenceBundleRepository,
        OnboardingEvidenceFileRepositoryPort evidenceFileRepository,
        TenantRepositoryPort tenantRepository,
        GroupRepositoryPort groupRepository,
        MembershipProvisioningRepositoryPort membershipProvisioningRepository,
        ObjectStoragePort objectStoragePort,
        Clock clock
    ) {
        this.applicationRepository = applicationRepository;
        this.evidenceBundleRepository = evidenceBundleRepository;
        this.evidenceFileRepository = evidenceFileRepository;
        this.tenantRepository = tenantRepository;
        this.groupRepository = groupRepository;
        this.membershipProvisioningRepository = membershipProvisioningRepository;
        this.objectStoragePort = objectStoragePort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ApplicationResult createBrandApplication(AuthPrincipal principal, CreateBrandApplicationCommand command) {
        requireReadyEvidenceOwnedByPrincipal(principal.userId(), command.evidenceBundleId());
        assertUniqueBrand(command.brandName(), command.bizRegNo());
        OrganizationApplication application = OrganizationApplication.createBrand(
            principal.userId(),
            command.brandName(),
            command.country(),
            command.bizRegNo(),
            command.evidenceBundleId()
        );
        return toApplicationResult(applicationRepository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationView> listBrandApplications() {
        return applicationRepository.findByType(GroupType.BRAND).stream()
            .map(this::toApplicationView)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationView getBrandApplication(String applicationId) {
        return toApplicationView(findBrandApplication(applicationId));
    }


    //TODO("이메일 발송로직")
    @Override
    @Transactional
    public ApproveApplicationResult approveBrandApplication(AuthPrincipal principal, String applicationId) {
        OrganizationApplication app = findBrandApplication(applicationId);
        app.assertPending();

        String tenantId = UUID.randomUUID().toString();
        String groupId = UUID.randomUUID().toString();
        String membershipId = UUID.randomUUID().toString();

        tenantRepository.save(new Tenant(tenantId, app.orgName(), app.country(), TenantStatus.ACTIVE));
        groupRepository.save(new Group(groupId, tenantId, GroupType.BRAND, GroupStatus.ACTIVE));
        membershipProvisioningRepository.save(new Membership(
            membershipId,
            app.applicantUserId(),
            groupId,
            tenantId,
            GroupType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));

        applicationRepository.save(app.approve(principal.userId(), tenantId, Instant.now(clock)));

        return new ApproveApplicationResult(tenantId, groupId, membershipId);
    }

    @Override
    @Transactional
    public ApplicationResult rejectBrandApplication(AuthPrincipal principal, String applicationId, String rejectReason) {
        OrganizationApplication app = findBrandApplication(applicationId);
        return toApplicationResult(applicationRepository.save(app.reject(principal.userId(), rejectReason, Instant.now(clock))));
    }

    @Override
    @Transactional
    public ApplicationResult createRetailApplication(
        AuthPrincipal principal,
        String tenantId,
        CreateRetailApplicationCommand command
    ) {
        requireReadyEvidenceOwnedByPrincipal(principal.userId(), command.evidenceBundleId());
        assertUniqueRetail(tenantId, command.retailName(), command.bizRegNo());
        OrganizationApplication application = OrganizationApplication.createRetail(
            principal.userId(),
            tenantId,
            command.retailName(),
            command.country(),
            command.bizRegNo(),
            command.evidenceBundleId()
        );
        return toApplicationResult(applicationRepository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationView getRetailApplication(String tenantId, String applicationId) {
        return toApplicationView(findRetailApplication(tenantId, applicationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationView> listRetailApplications(String tenantId) {
        return applicationRepository.findByTenantAndType(tenantId, GroupType.RETAIL).stream()
            .map(this::toApplicationView)
            .toList();
    }

    @Override
    @Transactional
    public ApproveApplicationResult approveRetailApplication(AuthPrincipal principal, String tenantId, String applicationId) {
        assertTenantIsolation(principal, tenantId);
        OrganizationApplication app = findRetailApplication(tenantId, applicationId);
        app.assertPending();

        String groupId = UUID.randomUUID().toString();
        String membershipId = UUID.randomUUID().toString();

        groupRepository.save(new Group(groupId, tenantId, GroupType.RETAIL, GroupStatus.ACTIVE));
        membershipProvisioningRepository.save(new Membership(
            membershipId,
            app.applicantUserId(),
            groupId,
            tenantId,
            GroupType.RETAIL,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));

        applicationRepository.save(app.approve(principal.userId(), tenantId, Instant.now(clock)));

        return new ApproveApplicationResult(tenantId, groupId, membershipId);
    }

    @Override
    @Transactional
    public ApplicationResult rejectRetailApplication(AuthPrincipal principal, String tenantId, String applicationId, String rejectReason) {
        assertTenantIsolation(principal, tenantId);
        OrganizationApplication app = findRetailApplication(tenantId, applicationId);
        return toApplicationResult(applicationRepository.save(app.reject(principal.userId(), rejectReason, Instant.now(clock))));
    }

    @Override
    @Transactional
    public PresignedEvidenceUploadResult presignEvidenceUpload(AuthPrincipal principal, PresignEvidenceUploadCommand command) {
        Instant now = Instant.now(clock);
        OnboardingEvidenceBundle bundle = resolveOrCreateBundle(principal, command.evidenceBundleId(), now);
        assertBundleFileLimit(bundle.evidenceBundleId());
        String objectKey = buildObjectKey(principal.userId(), bundle.evidenceBundleId(), command.fileName());

        OnboardingEvidenceFile evidenceFile = evidenceFileRepository.save(OnboardingEvidenceFile.start(
            bundle.evidenceBundleId(),
            command.fileName(),
            command.contentType(),
            objectKey,
            now
        ));

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
    @Transactional
    public EvidenceBundleResult completeEvidenceUpload(AuthPrincipal principal, CompleteEvidenceUploadCommand command) {
        requireText(command.evidenceBundleId(), "evidenceBundleId");
        requireText(command.evidenceFileId(), "evidenceFileId");
        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(command.evidenceBundleId())
            .orElseThrow(() -> new DomainException(ErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
        bundle.assertOwnedBy(principal.userId());

        OnboardingEvidenceFile evidenceFile = evidenceFileRepository.findById(command.evidenceFileId())
            .orElseThrow(() -> new DomainException(ErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
        evidenceFile.assertBelongsToBundle(bundle.evidenceBundleId());
        if (!objectStoragePort.objectExists(evidenceFile.objectKey())) {
            throw new DomainException(ErrorCode.EVIDENCE_NOT_FOUND, "Uploaded object not found");
        }

        evidenceFileRepository.save(
            evidenceFile.complete(command.sizeBytes(), Instant.now(clock))
        );

        List<OnboardingEvidenceFile> files = evidenceFileRepository.findByBundleId(bundle.evidenceBundleId());
        if (files.isEmpty()) {
            throw new DomainException(ErrorCode.INVALID_APPLICATION_STATE, "Evidence bundle has no files");
        }

        OnboardingEvidenceBundle currentBundle = bundle;
        boolean allReady = files.stream().allMatch(OnboardingEvidenceFile::isReady);
        if (allReady) {
            currentBundle = evidenceBundleRepository.save(bundle.markReady(Instant.now(clock)));
        }

        return new EvidenceBundleResult(currentBundle.evidenceBundleId(), currentBundle.status().name());
    }

    private void assertTenantIsolation(AuthPrincipal principal, String tenantId) {
        if (!TenantIsolationPolicy.isIsolated(principal.tenantId(), tenantId)) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant access denied");
        }
    }

    private OrganizationApplication findBrandApplication(String applicationId) {
        OrganizationApplication app = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new DomainException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
        if (app.type() != GroupType.BRAND) {
            throw new DomainException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found");
        }
        return app;
    }

    private OrganizationApplication findRetailApplication(String tenantId, String applicationId) {
        OrganizationApplication app = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new DomainException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
        if (app.type() != GroupType.RETAIL || !tenantId.equals(app.tenantId())) {
            throw new DomainException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found");
        }
        return app;
    }

    private void requireReadyEvidenceOwnedByPrincipal(String userId, String evidenceBundleId) {
        requireText(evidenceBundleId, "evidenceBundleId");
        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(evidenceBundleId)
            .orElseThrow(() -> new DomainException(ErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
        bundle.assertOwnedBy(userId);
        bundle.assertReady();
    }

    private void assertUniqueBrand(String orgName, String bizRegNo) {
        String normalizedOrgName = normalize(orgName);
        if (applicationRepository.existsBrandByOrgName(normalizedOrgName)) {
            throw new DomainException(ErrorCode.DUPLICATE_ORGANIZATION_NAME, "Brand name already exists");
        }
        String normalizedBizRegNo = normalizeOrNull(bizRegNo);
        if (normalizedBizRegNo != null && applicationRepository.existsBrandByBizRegNo(normalizedBizRegNo)) {
            throw new DomainException(ErrorCode.DUPLICATE_BIZ_REG_NO, "Business registration number already exists");
        }
    }

    private void assertUniqueRetail(String tenantId, String orgName, String bizRegNo) {
        String normalizedOrgName = normalize(orgName);
        if (applicationRepository.existsRetailByTenantAndOrgName(tenantId, normalizedOrgName)) {
            throw new DomainException(ErrorCode.DUPLICATE_ORGANIZATION_NAME, "Retail name already exists in tenant");
        }
        String normalizedBizRegNo = normalizeOrNull(bizRegNo);
        if (normalizedBizRegNo != null && applicationRepository.existsRetailByTenantAndBizRegNo(tenantId, normalizedBizRegNo)) {
            throw new DomainException(ErrorCode.DUPLICATE_BIZ_REG_NO, "Business registration number already exists in tenant");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOrNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ApplicationResult toApplicationResult(OrganizationApplication app) {
        return new ApplicationResult(
            app.applicationId(),
            app.type().name(),
            app.applicantUserId(),
            app.tenantId(),
            app.orgName(),
            app.country(),
            app.status().name(),
            app.rejectReason()
        );
    }

    private ApplicationView toApplicationView(OrganizationApplication app) {
        return new ApplicationView(
            app.applicationId(),
            app.type().name(),
            app.applicantUserId(),
            app.tenantId(),
            app.orgName(),
            app.country(),
            app.status().name(),
            app.rejectReason()
        );
    }

    private OnboardingEvidenceBundle resolveOrCreateBundle(AuthPrincipal principal, String requestedBundleId, Instant now) {
        if (requestedBundleId == null || requestedBundleId.isBlank()) {
            return evidenceBundleRepository.save(OnboardingEvidenceBundle.create(principal.userId(), now));
        }
        OnboardingEvidenceBundle bundle = evidenceBundleRepository.findById(requestedBundleId)
            .orElseThrow(() -> new DomainException(ErrorCode.EVIDENCE_NOT_FOUND, "Evidence not found"));
        bundle.assertOwnedBy(principal.userId());
        bundle.assertCollecting();
        return bundle;
    }

    private void assertBundleFileLimit(String bundleId) {
        int currentCount = evidenceFileRepository.findByBundleId(bundleId).size();
        if (currentCount >= MAX_EVIDENCE_FILES_PER_BUNDLE) {
            throw new DomainException(
                ErrorCode.EVIDENCE_FILE_LIMIT_EXCEEDED,
                "Maximum 5 evidence files are allowed per bundle"
            );
        }
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
}
