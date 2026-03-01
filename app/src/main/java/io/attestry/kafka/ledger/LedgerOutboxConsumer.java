package io.attestry.kafka.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.config.KafkaProperties;
import io.attestry.ledger.application.ledger.command.AppendLedgerEntryCommand;
import io.attestry.ledger.application.ledger.usecase.LedgerAppendUseCase;
import java.util.Collections;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerOutboxConsumer {

    private final ObjectMapper objectMapper;
    private final LedgerAppendUseCase ledgerAppendUseCase;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    public LedgerOutboxConsumer(
        ObjectMapper objectMapper,
        LedgerAppendUseCase ledgerAppendUseCase,
        KafkaTemplate<String, String> kafkaTemplate,
        KafkaProperties kafkaProperties
    ) {
        this.objectMapper = objectMapper;
        this.ledgerAppendUseCase = ledgerAppendUseCase;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    @KafkaListener(
        topics = "${app.kafka.topics.ledger-outbox}",
        groupId = "${app.kafka.consumer-group-id}"
    )
    public void consume(String payload) {
        try {
            LedgerOutboxEventPayload event = objectMapper.readValue(payload, LedgerOutboxEventPayload.class);
            ledgerAppendUseCase.append(new AppendLedgerEntryCommand(
                event.passportId(),
                event.eventCategory(),
                event.eventAction(),
                event.actorRole(),
                event.actorId(),
                event.occurredAt(),
                event.payload() == null ? Collections.emptyMap() : event.payload(),
                event.idempotencyKey()
            ));
        } catch (Exception ex) {
            kafkaTemplate.send(kafkaProperties.getTopics().getLedgerDlq(), payload);
        }
    }
}
