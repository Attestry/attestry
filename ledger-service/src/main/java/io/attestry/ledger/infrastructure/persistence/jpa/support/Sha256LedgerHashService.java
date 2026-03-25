package io.attestry.ledger.infrastructure.persistence.jpa.support;

import io.attestry.ledger.domain.ledger.service.LedgerHashService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class Sha256LedgerHashService implements LedgerHashService {

    @Override
    public String dataHash(String payloadCanonical) {
        return sha256Hex(payloadCanonical);
    }

    @Override
    public String entryHash(
        String prevHash,
        String dataHash,
        long seq,
        String eventCategory,
        String eventAction,
        String actorRole,
        String actorId,
        Instant occurredAt
    ) {
        String source = String.join("|",
            normalizeNullable(prevHash),
            dataHash,
            Long.toString(seq),
            eventCategory,
            eventAction,
            actorRole,
            actorId,
            occurredAt.toString()
        );
        return sha256Hex(source);
    }

    private String normalizeNullable(String value) {
        return value == null ? "" : value;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
