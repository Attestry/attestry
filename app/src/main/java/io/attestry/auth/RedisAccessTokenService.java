package io.attestry.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.config.AccessTokenProperties;
import io.attestry.userauth.application.port.auth.AccessTokenPort;
import io.attestry.userauth.security.AuthPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.auth.token", name = "provider", havingValue = "REDIS")
public class RedisAccessTokenService implements AccessTokenPort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AccessTokenProperties properties;
    private final Clock clock;

    public RedisAccessTokenService(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        AccessTokenProperties properties,
        Clock clock
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String issue(AuthPrincipal principal) {
        String token = UUID.randomUUID().toString();
        Duration ttl = Duration.between(Instant.now(clock), principal.expiresAt());
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofSeconds(1);
        }
        redisTemplate.opsForValue().set(key(token), write(principal), ttl);
        return token;
    }

    @Override
    public Optional<AuthPrincipal> parse(String token) {
        String payload = redisTemplate.opsForValue().get(key(token));
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        AuthPrincipal principal = read(payload);
        if (principal.expiresAt().isBefore(Instant.now(clock))) {
            revoke(token);
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    @Override
    public void revoke(String token) {
        redisTemplate.delete(key(token));
    }

    private String key(String token) {
        return properties.getRedisKeyPrefix() + token;
    }

    private String write(AuthPrincipal principal) {
        try {
            return objectMapper.writeValueAsString(principal);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize auth principal", ex);
        }
    }

    private AuthPrincipal read(String payload) {
        try {
            return objectMapper.readValue(payload, AuthPrincipal.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize auth principal", ex);
        }
    }
}
