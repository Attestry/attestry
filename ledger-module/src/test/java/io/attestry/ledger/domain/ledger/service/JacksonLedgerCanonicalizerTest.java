package io.attestry.ledger.domain.ledger.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.ledger.infrastructure.persistence.jpa.JacksonLedgerCanonicalizer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JacksonLedgerCanonicalizerTest {

    private final JacksonLedgerCanonicalizer canonicalizer = new JacksonLedgerCanonicalizer(new ObjectMapper());

    @Test
    void canonicalizeSortsNestedKeysDeterministically() {
        Map<String, Object> payload = Map.of(
            "serialNumber", "123",
            "asset", Map.of("modelId", "M55", "assetId", "A100"),
            "events", List.of(
                Map.of("z", 1, "a", 2),
                Map.of("b", true, "a", false)
            )
        );

        String canonical = canonicalizer.canonicalize(payload);

        assertEquals(
            "{\"asset\":{\"assetId\":\"A100\",\"modelId\":\"M55\"},\"events\":[{\"a\":2,\"z\":1},{\"a\":false,\"b\":true}],\"serialNumber\":\"123\"}",
            canonical
        );
    }
}
