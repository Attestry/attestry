package io.attestry.product.application.dto.result;

public record EvidenceFileResult(
    String evidenceId,
    String originalFileName,
    String contentType,
    long sizeBytes,
    String downloadUrl
) {
}
