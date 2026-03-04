package io.attestry.product.infrastructure.service;

import io.attestry.product.domain.service.UuidV7Generator;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefaultUuidV7Generator implements UuidV7Generator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String nextId() {
        long timestamp = Instant.now().toEpochMilli() & 0xFFFFFFFFFFFFL;
        long msb = (timestamp << 16) | 0x7000L | (random.nextInt(1 << 12) & 0x0FFFL);
        long lsb = 0x8000000000000000L | (random.nextLong() & 0x3FFFFFFFFFFFFFFFL);
        return new UUID(msb, lsb).toString();
    }
}
