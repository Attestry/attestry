package io.attestry.storage;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public ObjectStoragePort objectStoragePort(StorageProperties properties) {
        Assert.notNull(properties.getProvider(), "app.storage.provider is required");
        return switch (properties.getProvider()) {
            case MINIO -> new MinioObjectStorageAdapter(properties);
            case S3 -> new S3ObjectStorageAdapter(properties);
            case MEMORY -> new InMemoryObjectStorageAdapter();
        };
    }
}
