package io.attestry.kafka.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.config.KafkaProperties;
import io.attestry.ledger.application.ledger.command.AppendLedgerEntryCommand;
import io.attestry.ledger.application.usecase.LedgerAppendUseCase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerOutboxConsumer {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxConsumer.class);

    private final ObjectMapper objectMapper;
    private final LedgerAppendUseCase ledgerAppendUseCase;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties kafkaProperties;
    private final Counter dlqCounter;

    public LedgerOutboxConsumer(
        ObjectMapper objectMapper,
        LedgerAppendUseCase ledgerAppendUseCase,
        KafkaTemplate<String, String> kafkaTemplate,
        KafkaProperties kafkaProperties,
        MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.ledgerAppendUseCase = ledgerAppendUseCase;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.dlqCounter = Counter.builder("outbox.dlq.count")
            .register(meterRegistry);
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
            log.debug("ledger outbox consumed: passportId={}, eventCategory={}, eventAction={}",
                event.passportId(), event.eventCategory(), event.eventAction());
        } catch (Exception ex) {
            dlqCounter.increment();
            ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
                kafkaProperties.getTopics().getLedgerDlq(), payload);
            dlqRecord.headers()
                .add("x-error-message", safeBytes(ex.getMessage()))
                .add("x-error-type", safeBytes(ex.getClass().getName()))
                .add("x-failed-at", safeBytes(Instant.now().toString()));
            kafkaTemplate.send(dlqRecord);
            log.warn("ledger outbox DLQ: errorType={}, errorMessage={}",
                ex.getClass().getName(), ex.getMessage());
        }
    }

    private byte[] safeBytes(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }
}
