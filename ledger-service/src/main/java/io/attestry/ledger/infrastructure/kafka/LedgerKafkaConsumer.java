package io.attestry.ledger.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.ledger.application.ledger.command.AppendLedgerEntryCommand;
import io.attestry.ledger.application.usecase.LedgerAppendUseCase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ledger.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(LedgerKafkaConsumer.class);

    private final ObjectMapper objectMapper;
    private final LedgerAppendUseCase ledgerAppendUseCase;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final LedgerKafkaProperties kafkaProperties;
    private final Counter dlqCounter;
    private final Counter consumeSuccessCounter;
    private final Counter consumeFailureCounter;
    private final Timer consumeTimer;
    private final Timer consumeLagTimer;

    public LedgerKafkaConsumer(
        ObjectMapper objectMapper,
        LedgerAppendUseCase ledgerAppendUseCase,
        KafkaTemplate<String, String> kafkaTemplate,
        LedgerKafkaProperties kafkaProperties,
        MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.ledgerAppendUseCase = ledgerAppendUseCase;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.dlqCounter = Counter.builder("outbox.dlq.count")
            .register(meterRegistry);
        this.consumeSuccessCounter = Counter.builder("ledger.kafka.consume.count")
            .tag("result", "success")
            .register(meterRegistry);
        this.consumeFailureCounter = Counter.builder("ledger.kafka.consume.count")
            .tag("result", "failure")
            .register(meterRegistry);
        this.consumeTimer = Timer.builder("ledger.kafka.consume.duration")
            .register(meterRegistry);
        this.consumeLagTimer = Timer.builder("ledger.kafka.consume.lag")
            .register(meterRegistry);
    }

    @KafkaListener(
        topics = "${app.ledger.kafka.topics.ledger-outbox}",
        groupId = "${app.ledger.kafka.consumer-group-id}",
        concurrency = "${app.ledger.kafka.listener-concurrency:1}"
    )
    public void consume(String payload) {
        Timer.Sample sample = Timer.start();
        try {
            doConsume(payload);
            sample.stop(consumeTimer);
            consumeSuccessCounter.increment();
        } catch (Exception ex) {
            sample.stop(consumeTimer);
            dlqCounter.increment();
            consumeFailureCounter.increment();
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

    private void doConsume(String payload) throws Exception {
        OutboxEventEnvelope event = objectMapper.readValue(payload, OutboxEventEnvelope.class);
        if (event.occurredAt() != null) {
            consumeLagTimer.record(Duration.between(event.occurredAt(), Instant.now()).abs());
        }
        Map<String, Object> normalizedPayload = normalizePayload(event.payload());
        String actorRole = normalizeActorRole(event.actorRole(), normalizedPayload, event.passportId(), event.idempotencyKey());
        ledgerAppendUseCase.append(new AppendLedgerEntryCommand(
            event.passportId(),
            event.eventCategory(),
            event.eventAction(),
            actorRole,
            event.actorId(),
            event.occurredAt(),
            normalizedPayload,
            event.idempotencyKey()
        ));
        log.debug("ledger outbox consumed: passportId={}, eventCategory={}, eventAction={}",
            event.passportId(), event.eventCategory(), event.eventAction());
    }

    private Map<String, Object> normalizePayload(Map<String, Object> payload) {
        return payload == null ? Collections.emptyMap() : payload;
    }

    private String normalizeActorRole(
        String actorRole,
        Map<String, Object> payload,
        String passportId,
        String idempotencyKey
    ) {
        if (actorRole != null && !actorRole.isBlank()) {
            return actorRole;
        }

        Object legacyActorRole = payload.get("actorRole");
        if (legacyActorRole instanceof String value && !value.isBlank()) {
            log.info("ledger outbox legacy actorRole normalized: passportId={}, idempotencyKey={}",
                passportId, idempotencyKey);
            return value;
        }
        return actorRole;
    }

    private byte[] safeBytes(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }
}
