package io.attestry.workflow.application.port.common;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkflowEvidencePort {

    void createEvidenceGroupIfAbsent(String evidenceGroupId, String tenantId, String ownerUserId, Instant now);

    void createPendingEvidence(
        String evidenceId,
        String evidenceGroupId,
        String objectKey,
        String originalFileName,
        String contentType,
        Instant now
    );

    Optional<EvidenceRecord> findEvidenceById(String evidenceGroupId, String evidenceId);

    void markEvidenceReady(
        String evidenceGroupId,
        String evidenceId,
        long sizeBytes,
        String fileHash,
        Instant now
    );

    Optional<EvidenceGroupScopeRecord> findEvidenceGroupScope(String evidenceGroupId);

    List<String> findReadyEvidenceHashes(String evidenceGroupId);

    List<EvidenceRecord> findEvidenceByEvidenceGroupId(String evidenceGroupId);

    record EvidenceRecord(
        String evidenceId,
        String evidenceGroupId,
        String fileHash,
        String objectKey,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String status
    ) {
    }

    record EvidenceGroupScopeRecord(
        String evidenceGroupId,
        String tenantId,
        String ownerUserId
    ) {
    }
}
