package io.attestry.userauth.application.onboarding.command;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.onboarding.result.EvidenceBundleResult;
import io.attestry.userauth.application.onboarding.result.PresignedEvidenceUploadResult;
import io.attestry.userauth.application.port.onboarding.OnboardingEvidenceBundleRepositoryPort;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingEvidenceCommandExecutor {

    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    private final OnboardingEvidenceBundleRepositoryPort evidenceBundleRepository;
    private final OnboardingApplicationCommandPolicy commandPolicy;
    private final ObjectStoragePort objectStoragePort;
    private final OnboardingEvidenceUploadCompletionTxService uploadCompletionTxService;
    private final Clock clock;

    public PresignedEvidenceUploadResult presignEvidenceUpload(ActorContext actor, PresignEvidenceUploadCommand command) {
        Instant now = Instant.now(clock);
        OnboardingEvidenceBundle bundle = commandPolicy.resolveOrCreateBundle(actor, command.evidenceBundleId(), now);
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

    public EvidenceBundleResult completeEvidenceUpload(ActorContext actor, CompleteEvidenceUploadCommand command) {
        OnboardingEvidenceFile evidenceFile = evidenceBundleRepository.findFileById(command.evidenceFileId())
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_NOT_FOUND, "Evidence file not found"));
        if (!objectStoragePort.objectExists(evidenceFile.objectKey())) {
            throw new UserAuthDomainException(UserAuthErrorCode.EVIDENCE_NOT_FOUND, "Uploaded object not found");
        }

        return uploadCompletionTxService.completeEvidenceUpload(actor, command);
    }

    private String buildObjectKey(String userId, String bundleId, String fileName) {
        String safeFileName = fileName == null ? "evidence.bin" : fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "onboarding/" + userId + "/" + bundleId + "/" + UUID.randomUUID() + "/" + safeFileName;
    }
}
