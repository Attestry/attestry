package io.attestry.kafka.workflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.product.application.port.projection.ProductDistributionProjectionWritePort;
import io.attestry.product.application.port.projection.ProductDistributionProjectionWritePort.DistributionPayload;
import io.attestry.product.application.port.projection.ProductRetailAccessProjectionWritePort;
import io.attestry.product.application.port.projection.ProductRetailAccessProjectionWritePort.RetailAccessPayload;
import io.attestry.product.application.port.projection.ProductShipmentProjectionWritePort;
import io.attestry.product.application.port.projection.ProductShipmentProjectionWritePort.ShipmentPayload;
import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort;
import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort.ProductStatePayload;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CompletableFuture;
import java.time.Instant;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class WorkflowProductProjectionConsumerTest {

    @Mock private WorkflowPassportProjectionWritePort projectionWriter;
    @Mock private ProductShipmentProjectionWritePort shipmentProjectionWriter;
    @Mock private ProductDistributionProjectionWritePort distributionProjectionWriter;
    @Mock private ProductRetailAccessProjectionWritePort retailAccessProjectionWriter;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private WorkflowProductProjectionConsumer consumer;

    @BeforeEach
    void setUp() {
        WorkflowReadProjectionKafkaProperties properties = new WorkflowReadProjectionKafkaProperties();
        properties.setDlqTopic("workflow-projection.dlq");
        org.mockito.Mockito.lenient().when(kafkaTemplate.send(org.mockito.ArgumentMatchers.<ProducerRecord<String, String>>any()))
            .thenReturn(CompletableFuture.completedFuture(
                new SendResult<>(new ProducerRecord<>("workflow-projection.dlq", "payload"), (RecordMetadata) null)
            ));
        consumer = new WorkflowProductProjectionConsumer(
            new ObjectMapper(),
            projectionWriter,
            shipmentProjectionWriter,
            distributionProjectionWriter,
            retailAccessProjectionWriter,
            kafkaTemplate,
            properties,
            new SimpleMeterRegistry()
        );
    }

    @Test
    void consume_refreshesProductProjection() {
        consumer.consume("""
            {
              "aggregateType": "PRODUCT",
              "passportId": "passport-1",
              "eventCategory": "GENESIS",
              "eventAction": "MINTED",
              "idempotencyKey": "event-1",
              "occurredAt": "2026-03-12T00:00:00Z",
              "payload": {
                "tenantId": "t1",
                "assetId": "a1",
                "assetState": "ACTIVE",
                "riskFlagProjection": "NONE",
                "serialNumber": "SN001",
                "modelId": "M1",
                "modelName": "Model",
                "productionBatch": "B1",
                "factoryCode": "F1",
                "manufacturedAt": "2026-01-01T00:00:00Z"
              }
            }
            """);

        verify(projectionWriter)
            .refreshStateAndCatalog(any(ProductStatePayload.class), any(), any(), any());
        verify(shipmentProjectionWriter, never()).refreshShipmentProjection(any(), any(), any(), any());
        verify(distributionProjectionWriter, never()).refreshDistributionProjection(any(), any(), any(), any());
        verify(retailAccessProjectionWriter, never()).refreshB2cTransferAccess(any(), any(), any());
    }

    @Test
    void consume_refreshesShipmentProjection() {
        consumer.consume("""
            {
              "aggregateType": "SHIPMENT",
              "passportId": "passport-2",
              "eventCategory": "SHIPMENT",
              "eventAction": "RELEASED",
              "idempotencyKey": "event-2",
              "occurredAt": "2026-03-12T01:00:00Z",
              "payload": {
                "shipmentId": "s1",
                "status": "RELEASED",
                "shipmentRound": 1,
                "releasedAt": "2026-03-12T01:00:00Z",
                "releasedByUserDisplay": "user@test.com"
              }
            }
            """);

        verify(shipmentProjectionWriter)
            .refreshShipmentProjection(any(ShipmentPayload.class), any(), any(), any());
        verify(projectionWriter, never()).refreshStateAndCatalog(any(), any(), any(), any());
    }

    @Test
    void consume_refreshesDistributionProjection() {
        consumer.consume("""
            {
              "aggregateType": "DISTRIBUTION",
              "passportId": "passport-3",
              "eventCategory": "DISTRIBUTION",
              "eventAction": "CREATED",
              "idempotencyKey": "event-3",
              "occurredAt": "2026-03-12T02:00:00Z",
              "payload": {
                "distributionId": "d1",
                "targetTenantId": "tt1",
                "targetTenantName": "Retail",
                "targetTenantType": "RETAIL",
                "partnerLinkId": "pl1",
                "status": "DISTRIBUTED",
                "distributedAt": "2026-03-12T02:00:00Z"
              }
            }
            """);

        verify(distributionProjectionWriter)
            .refreshDistributionProjection(any(DistributionPayload.class), any(), any(), any());
        verify(projectionWriter, never()).refreshStateAndCatalog(any(), any(), any(), any());
    }

    @Test
    void consume_refreshesRetailAccessProjection() {
        consumer.consume("""
            {
              "aggregateType": "TRANSFER",
              "passportId": "passport-4",
              "eventCategory": "OWNERSHIP",
              "eventAction": "CLAIMED",
              "idempotencyKey": "event-4",
              "occurredAt": "2026-03-12T03:00:00Z",
              "payload": {
                "transferId": "transfer-1",
                "tenantId": "t1",
                "completedAt": "2026-03-12T03:00:00Z"
              }
            }
            """);

        verify(retailAccessProjectionWriter)
            .refreshB2cTransferAccess(any(RetailAccessPayload.class), any(), any());
        verify(projectionWriter, never()).refreshStateAndCatalog(any(), any(), any(), any());
    }

    @Test
    void consume_ignoresEventWithoutPassportId() {
        consumer.consume("""
            {
              "aggregateType": "PRODUCT",
              "eventCategory": "GENESIS",
              "eventAction": "MINTED"
            }
            """);

        verify(projectionWriter, never()).refreshStateAndCatalog(any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(org.mockito.ArgumentMatchers.<ProducerRecord<String, String>>any());
    }

    @Test
    void consume_sendsDlqWhenRefreshFails() {
        doThrow(new IllegalStateException("projection failed"))
            .when(projectionWriter)
            .refreshStateAndCatalog(any(), any(), any(), any());

        consumer.consume("""
            {
              "aggregateType": "PRODUCT",
              "passportId": "passport-5",
              "eventCategory": "RISK",
              "eventAction": "FLAGGED",
              "idempotencyKey": "event-5",
              "occurredAt": "2026-03-12T04:00:00Z",
              "payload": {
                "tenantId": "t1",
                "assetId": "a1",
                "assetState": "ACTIVE",
                "riskFlagProjection": "FLAGGED"
              }
            }
            """);

        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.<ProducerRecord<String, String>>any());
    }
}
