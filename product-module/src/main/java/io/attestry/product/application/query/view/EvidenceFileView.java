package io.attestry.product.application.query.view;

public record EvidenceFileView(
    String evidenceId,
    String originalFileName,
    String contentType,
    long sizeBytes,
    String downloadUrl
) {
}
