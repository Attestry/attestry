package io.attestry.storage;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import java.time.Duration;
import java.time.Instant;
import org.springframework.util.Assert;

public class MinioObjectStorageAdapter implements ObjectStoragePort {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioObjectStorageAdapter(StorageProperties properties) {
        Assert.hasText(properties.getBucket(), "app.storage.bucket is required");
        Assert.hasText(properties.getMinio().getEndpoint(), "app.storage.minio.endpoint is required");
        Assert.hasText(properties.getMinio().getAccessKey(), "app.storage.minio.access-key is required");
        Assert.hasText(properties.getMinio().getSecretKey(), "app.storage.minio.secret-key is required");

        this.bucket = properties.getBucket();
        this.minioClient = MinioClient.builder()
            .endpoint(properties.getMinio().getEndpoint())
            .credentials(properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey())
            .build();
    }

    @Override
    public PresignedUpload issuePresignedUpload(String objectKey, String contentType, Duration ttl) {
        try {
            int expirySeconds = (int) ttl.getSeconds();
            String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(expirySeconds)
                    .build()
            );
            return new PresignedUpload(url, Instant.now().plusSeconds(expirySeconds));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to issue MinIO presigned upload URL", ex);
        }
    }

    @Override
    public boolean objectExists(String objectKey) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build()
            );
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public PresignedDownload issuePresignedDownload(String objectKey, Duration ttl) {
        try {
            int expirySeconds = (int) ttl.getSeconds();
            String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(expirySeconds)
                    .build()
            );
            return new PresignedDownload(url, Instant.now().plusSeconds(expirySeconds));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to issue MinIO presigned download URL", ex);
        }
    }
}
