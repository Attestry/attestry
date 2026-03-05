package io.attestry.workflow.application.support;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
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

    public String buildObjectKey(String pathPrefix, String tenantId, String groupId, String evidenceGroupId, String fileName) {
        return pathPrefix + tenantId + "/" + groupId + "/" + evidenceGroupId + "/" + UUID.randomUUID() + "/" + fileName;
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
}
