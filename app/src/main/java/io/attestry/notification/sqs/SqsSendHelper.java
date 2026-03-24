package io.attestry.notification.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

final class SqsSendHelper {

    private SqsSendHelper() {
    }

    static void send(
        SqsClient sqsClient,
        ObjectMapper objectMapper,
        String queueUrl,
        String fifoMessageGroupId,
        String dedupeKey,
        Object messagePayload
    ) {
        String body;
        try {
            body = objectMapper.writeValueAsString(messagePayload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize SQS message", ex);
        }

        SendMessageRequest.Builder request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(body);

        if (queueUrl.endsWith(".fifo")) {
            request.messageGroupId(fifoMessageGroupId);
            request.messageDeduplicationId(dedupeKey);
        }

        sqsClient.sendMessage(request.build());
    }
}
