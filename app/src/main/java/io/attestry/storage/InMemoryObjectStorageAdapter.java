package io.attestry.storage;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryObjectStorageAdapter implements ObjectStoragePort {

    private final Set<String> knownObjectKeys = ConcurrentHashMap.newKeySet();

    @Override
    public PresignedUpload issuePresignedUpload(String objectKey, String contentType, Duration ttl) {
        knownObjectKeys.add(objectKey);
        Instant expiresAt = Instant.now().plus(ttl);
        String fakeUrl = "http://localhost:8080/storage/mock-upload/" + objectKey;
        return new PresignedUpload(fakeUrl, expiresAt);
    }

    @Override
    public boolean objectExists(String objectKey) {
        return knownObjectKeys.contains(objectKey);
    }

    @Override
    public PresignedDownload issuePresignedDownload(String objectKey, Duration ttl) {
        knownObjectKeys.add(objectKey);
        Instant expiresAt = Instant.now().plus(ttl);
        String fakeUrl = "http://localhost:8080/storage/mock-download/" + objectKey;
        return new PresignedDownload(fakeUrl, expiresAt);
    }
}
