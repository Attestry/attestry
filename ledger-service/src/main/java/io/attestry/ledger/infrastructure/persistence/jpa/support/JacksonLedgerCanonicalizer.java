package io.attestry.ledger.infrastructure.persistence.jpa.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.ledger.domain.ledger.service.LedgerCanonicalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class JacksonLedgerCanonicalizer implements LedgerCanonicalizer {

    private final ObjectMapper objectMapper;

    public JacksonLedgerCanonicalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String canonicalize(Map<String, Object> payload) {
        return writeJson(normalize(payload));
    }

    @Override
    public String serialize(Map<String, Object> payload) {
        return writeJson(payload);
    }

    @SuppressWarnings("unchecked")
    private Object normalize(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
            }
            return sorted;
        }
        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            for (Object item : list) {
                normalized.add(normalize(item));
            }
            return normalized;
        }
        return value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("payload cannot be serialized", ex);
        }
    }
}
