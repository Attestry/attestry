package io.attestry.userauth.application.port;

import java.time.Duration;
import java.time.Instant;

public interface ObjectStoragePort {
    PresignedUpload issuePresignedUpload(String objectKey, String contentType, Duration ttl);

    PresignedDownload issuePresignedDownload(String objectKey, Duration ttl);

    boolean objectExists(String objectKey);

    record PresignedUpload(String uploadUrl, Instant expiresAt) {
    }

    record PresignedDownload(String downloadUrl, Instant expiresAt) {
    }
}
