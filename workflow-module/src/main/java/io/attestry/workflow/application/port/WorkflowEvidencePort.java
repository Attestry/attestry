package io.attestry.workflow.application.port;

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

    Optional<EvidenceView> findEvidenceById(String evidenceGroupId, String evidenceId);

    void markEvidenceReady(
        String evidenceGroupId,
        String evidenceId,
        long sizeBytes,
        String fileHash,
        Instant now
    );

    Optional<EvidenceGroupScopeView> findEvidenceGroupScope(String evidenceGroupId);

    List<String> findReadyEvidenceHashes(String evidenceGroupId);

    List<EvidenceView> findEvidenceByEvidenceGroupId(String evidenceGroupId);

    record EvidenceView(
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

    record EvidenceGroupScopeView(
        String evidenceGroupId,
        String tenantId,
        String ownerUserId
    ) {
    }
}
