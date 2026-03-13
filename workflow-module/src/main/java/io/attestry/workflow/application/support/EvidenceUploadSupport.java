package io.attestry.workflow.application.support;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EvidenceUploadSupport {

    public String normalizeHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "fileHash is required");
        }
        String normalized = hash.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-f0-9]{64}$")) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "fileHash must be sha256 hex");
        }
        return normalized;
    }

    public String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "fileName is required");
        }
        return fileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "contentType is required");
        }
        return contentType.trim();
    }

    public String buildObjectKey(String pathPrefix, String tenantId, String evidenceGroupId, String fileName) {
        return pathPrefix + tenantId + "/" + evidenceGroupId + "/" + UUID.randomUUID() + "/" + fileName;
    }

    public void assertObjectUploaded(boolean objectExists) {
        if (!objectExists) {
            throw new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Uploaded object not found");
        }
    }

    public void assertPositiveSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "sizeBytes must be positive");
        }
    }

    public PresignedEvidenceUploadResult doPresign(
        WorkflowEvidencePort evidencePort,
        ObjectStoragePort objectStoragePort,
        String objectKeyPrefix, Duration presignTtl,
        String tenantId, String userId,
        String evidenceGroupIdInput, String fileName, String contentType,
        Instant now
    ) {
        boolean isNewGroup = evidenceGroupIdInput == null || evidenceGroupIdInput.isBlank();
        String evidenceGroupId = isNewGroup
            ? UUID.randomUUID().toString()
            : evidenceGroupIdInput.trim();

        if (isNewGroup) {
            evidencePort.createEvidenceGroupIfAbsent(evidenceGroupId, tenantId, userId, now);
        } else {
            assertEvidenceGroupScope(evidencePort, evidenceGroupId, tenantId);
        }

        String safeFileName = normalizeFileName(fileName);
        String normalizedContentType = normalizeContentType(contentType);
        String objectKey = buildObjectKey(objectKeyPrefix, tenantId, evidenceGroupId, safeFileName);
        String evidenceId = UUID.randomUUID().toString();

        evidencePort.createPendingEvidence(evidenceId, evidenceGroupId, objectKey, safeFileName, normalizedContentType, now);

        ObjectStoragePort.PresignedUpload presigned = objectStoragePort.issuePresignedUpload(objectKey, normalizedContentType, presignTtl);
        return new PresignedEvidenceUploadResult(
            evidenceGroupId, evidenceId, objectKey, presigned.uploadUrl(), presigned.expiresAt()
        );
    }

    public EvidenceCompleteResult doComplete(
        WorkflowEvidencePort evidencePort,
        ObjectStoragePort objectStoragePort,
        String evidenceGroupId, String evidenceId,
        long sizeBytes, String fileHash, Instant now
    ) {
        WorkflowEvidencePort.EvidenceRecord evidence = evidencePort.findEvidenceById(
            evidenceGroupId, evidenceId
        ).orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Evidence file not found"));

        assertObjectUploaded(objectStoragePort.objectExists(evidence.objectKey()));
        assertPositiveSize(sizeBytes);

        String normalizedHash = normalizeHash(fileHash);
        evidencePort.markEvidenceReady(evidenceGroupId, evidenceId, sizeBytes, normalizedHash, now);

        return new EvidenceCompleteResult(evidenceGroupId, evidenceId, "READY");
    }

    public void assertEvidenceGroupScope(
        WorkflowEvidencePort evidencePort,
        String evidenceGroupId, String tenantId
    ) {
        WorkflowEvidencePort.EvidenceGroupScopeRecord scope = evidencePort
            .findEvidenceGroupScope(evidenceGroupId)
            .orElseThrow(() -> new WorkflowDomainException(
                WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Evidence group not found"));
        if (!tenantId.equals(scope.tenantId())) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.TENANT_ISOLATION_VIOLATION,
                "Cross-tenant evidence group access denied");
        }
    }
}
