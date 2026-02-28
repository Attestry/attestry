package io.attestry.storage;

import io.attestry.userauth.application.port.ObjectStoragePort;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class S3ObjectStorageAdapter implements ObjectStoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3ObjectStorageAdapter(StorageProperties properties) {
        Assert.hasText(properties.getBucket(), "app.storage.bucket is required");
        Assert.hasText(properties.getS3().getRegion(), "app.storage.s3.region is required");
        this.bucket = properties.getBucket();

        Region region = Region.of(properties.getS3().getRegion());
        S3ClientBuilder clientBuilder = S3Client.builder()
            .region(region)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(false).build());
        S3Presigner.Builder presignerBuilder = S3Presigner.builder().region(region);

        if (StringUtils.hasText(properties.getS3().getEndpoint())) {
            URI endpoint = URI.create(properties.getS3().getEndpoint());
            clientBuilder.endpointOverride(endpoint);
            presignerBuilder.endpointOverride(endpoint);
        }

        if (StringUtils.hasText(properties.getS3().getAccessKey()) && StringUtils.hasText(properties.getS3().getSecretKey())) {
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getS3().getAccessKey(), properties.getS3().getSecretKey())
            );
            clientBuilder.credentialsProvider(credentialsProvider);
            presignerBuilder.credentialsProvider(credentialsProvider);
        } else {
            DefaultCredentialsProvider defaultCredentialsProvider = DefaultCredentialsProvider.create();
            clientBuilder.credentialsProvider(defaultCredentialsProvider);
            presignerBuilder.credentialsProvider(defaultCredentialsProvider);
        }

        this.s3Client = clientBuilder.build();
        this.s3Presigner = presignerBuilder.build();
    }

    @Override
    public PresignedUpload issuePresignedUpload(String objectKey, String contentType, Duration ttl) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(putObjectRequest)
                .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            return new PresignedUpload(presignedRequest.url().toString(), Instant.now().plus(ttl));
        } catch (SdkException ex) {
            throw new IllegalStateException("Failed to issue S3 presigned upload URL", ex);
        }
    }

    @Override
    public boolean objectExists(String objectKey) {
        try {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build()
            );
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (S3Exception ex) {
            return ex.statusCode() != 404 ? throwAsIllegalState(ex) : false;
        } catch (SdkException ex) {
            throw new IllegalStateException("Failed to check S3 object existence", ex);
        }
    }

    private boolean throwAsIllegalState(S3Exception ex) {
        throw new IllegalStateException("Failed to check S3 object existence", ex);
    }
}
