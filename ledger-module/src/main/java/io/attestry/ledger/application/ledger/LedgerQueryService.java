package io.attestry.ledger.application.ledger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.ledger.application.ledger.query.LedgerEntryView;
import io.attestry.ledger.application.port.LedgerQueryRepositoryPort;
import io.attestry.ledger.application.usecase.LedgerQueryUseCase;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerQueryService implements LedgerQueryUseCase {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final LedgerQueryRepositoryPort repository;
    private final ObjectMapper objectMapper;

    public LedgerQueryService(LedgerQueryRepositoryPort repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntryView> listByPassportId(String passportId) {
        requireText(passportId, "passportId");
        return repository.findByPassportIdOrderBySeqAsc(passportId).stream()
            .map(this::toView)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerEntryView getByPassportIdAndLedgerId(String passportId, String ledgerId) {
        requireText(passportId, "passportId");
        requireText(ledgerId, "ledgerId");
        return repository.findByPassportIdAndLedgerId(passportId, ledgerId)
            .map(this::toView)
            .orElseThrow(() -> new NoSuchElementException("ledger entry not found"));
    }

    private LedgerEntryView toView(LedgerEntry entry) {
        return new LedgerEntryView(
            entry.ledgerId(),
            entry.passportId(),
            entry.seq(),
            entry.eventCategory(),
            entry.eventAction(),
            entry.actorRole(),
            entry.actorId(),
            entry.occurredAt(),
            parsePayload(entry.payloadJson()),
            entry.dataHash(),
            entry.prevHash(),
            entry.entryHash()
        );
    }

    private Map<String, Object> parsePayload(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to parse payload_json", ex);
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
