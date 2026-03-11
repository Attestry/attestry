package io.attestry.product.application.dto.view;

public record EvidenceFileView(
    String evidenceId,
    String originalFileName,
    String contentType,
    long sizeBytes,
    String downloadUrl
) {
}
