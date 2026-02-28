package io.attestry.userauth.application.port;

import java.time.Duration;
import java.time.Instant;

public interface ObjectStoragePort {
    PresignedUpload issuePresignedUpload(String objectKey, String contentType, Duration ttl);

    boolean objectExists(String objectKey);

    record PresignedUpload(String uploadUrl, Instant expiresAt) {
    }
}
