package io.attestry.ledger.domain.ledger.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.ledger.infrastructure.persistence.jpa.support.JacksonLedgerCanonicalizer;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    @Test
    void emptyMap_canonicalizesToEmptyObject() {
        assertEquals("{}", canonicalizer.canonicalize(Collections.emptyMap()));
    }

    @Test
    void numericAndBooleanAndNullTypes_preserved() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("integer", 42);
        payload.put("decimal", 3.14);
        payload.put("flag", true);
        payload.put("nothing", null);

        String canonical = canonicalizer.canonicalize(payload);

        assertEquals("{\"decimal\":3.14,\"flag\":true,\"integer\":42,\"nothing\":null}", canonical);
    }

    @Test
    void unicodeStrings_preserved() {
        Map<String, Object> payload = Map.of(
            "name", "한글 테스트",
            "emoji", "✅"
        );

        String canonical = canonicalizer.canonicalize(payload);

        assertEquals("{\"emoji\":\"✅\",\"name\":\"한글 테스트\"}", canonical);
    }

    @Test
    void sameInput_alwaysProducesSameCanonical() {
        Map<String, Object> payload = Map.of("b", 2, "a", 1);
        String first = canonicalizer.canonicalize(payload);
        for (int i = 0; i < 100; i++) {
            assertEquals(first, canonicalizer.canonicalize(payload), "canonicalize must be deterministic");
        }
    }

    @Test
    void differentKeyOrder_producesSameCanonical() {
        Map<String, Object> map1 = new LinkedHashMap<>();
        map1.put("z", 1);
        map1.put("a", 2);

        Map<String, Object> map2 = new LinkedHashMap<>();
        map2.put("a", 2);
        map2.put("z", 1);

        assertEquals(canonicalizer.canonicalize(map1), canonicalizer.canonicalize(map2));
    }
}
