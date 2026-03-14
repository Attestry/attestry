package io.attestry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.token")
public class AccessTokenProperties {

    private Provider provider = Provider.MEMORY;
    private String redisKeyPrefix = "auth:access-token:";

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public enum Provider {
        MEMORY,
        REDIS
    }
}
