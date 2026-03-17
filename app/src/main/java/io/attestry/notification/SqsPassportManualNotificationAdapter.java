package io.attestry.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.userauth.application.port.notification.PassportManualNotificationPort;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@ConditionalOnProperty(
    prefix = "app.workflow.passport-manual.mail",
    name = "provider",
    havingValue = "SQS"
)
public class SqsPassportManualNotificationAdapter implements PassportManualNotificationPort {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String queueUrl;
    private final String fifoMessageGroupId;
    private final PassportManualAttachmentResolver attachmentResolver;

    public SqsPassportManualNotificationAdapter(
        ObjectMapper objectMapper,
        @Value("${app.workflow.passport-manual.mail.enabled:true}") boolean enabled,
        @Value("${app.workflow.passport-manual.sqs.queue-url:}") String queueUrl,
        @Value("${app.workflow.passport-manual.sqs.region:ap-northeast-2}") String region,
        @Value("${app.workflow.passport-manual.sqs.endpoint:}") String endpoint,
        @Value("${app.workflow.passport-manual.sqs.access-key:}") String accessKey,
        @Value("${app.workflow.passport-manual.sqs.secret-key:}") String secretKey,
        @Value("${app.workflow.passport-manual.sqs.fifo-message-group-id:passport-manual-notification}") String fifoMessageGroupId,
        PassportManualAttachmentResolver attachmentResolver
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.queueUrl = queueUrl;
        this.fifoMessageGroupId = fifoMessageGroupId;
        this.attachmentResolver = attachmentResolver;

        SqsClientBuilder builder = SqsClient.builder().region(Region.of(region));
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
            );
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        this.sqsClient = builder.build();
    }

    @Override
    public void send(PassportManualNotification notification) {
        if (!enabled) {
            return;
        }
        Assert.hasText(queueUrl, "app.workflow.passport-manual.sqs.queue-url is required when provider=SQS");

        List<ManualAttachment> attachments = attachmentResolver.resolveAttachments(notification);
        String body = serialize(notification, attachments);
        SendMessageRequest.Builder request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(body);

        if (queueUrl.endsWith(".fifo")) {
            request.messageGroupId(fifoMessageGroupId);
            request.messageDeduplicationId(notification.passportId() + ":" + notification.recipientEmail());
        }

        sqsClient.sendMessage(request.build());
    }

    private String serialize(PassportManualNotification notification, List<ManualAttachment> attachments) {
        try {
            return objectMapper.writeValueAsString(new PassportManualQueueMessage(
                "PASSPORT_MANUAL_DELIVERY",
                notification.passportId(),
                notification.recipientEmail(),
                notification.serialNumber(),
                notification.modelName(),
                notification.message(),
                attachments
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize passport manual notification", ex);
        }
    }

    private record PassportManualQueueMessage(
        String type,
        String passportId,
        String recipientEmail,
        String serialNumber,
        String modelName,
        String message,
        List<ManualAttachment> attachments
    ) {
    }
}
