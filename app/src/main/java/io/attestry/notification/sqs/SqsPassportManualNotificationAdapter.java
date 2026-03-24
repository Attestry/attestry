package io.attestry.notification.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.notification.PassportManualAttachmentResolver;
import io.attestry.notification.PassportManualNotificationProperties;
import io.attestry.userauth.application.port.notification.PassportManualNotificationPort;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
@ConditionalOnProperty(
    prefix = "app.workflow.passport-manual.mail",
    name = "provider",
    havingValue = "SQS"
)
public class SqsPassportManualNotificationAdapter implements PassportManualNotificationPort {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final PassportManualNotificationProperties properties;
    private final PassportManualAttachmentResolver attachmentResolver;

    public SqsPassportManualNotificationAdapter(
        ObjectMapper objectMapper,
        PassportManualNotificationProperties properties,
        PassportManualAttachmentResolver attachmentResolver
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.attachmentResolver = attachmentResolver;
        PassportManualNotificationProperties.Sqs sqs = properties.getSqs();
        this.sqsClient = SqsClientFactory.create(
            sqs.getRegion(), sqs.getEndpoint(), sqs.getAccessKey(), sqs.getSecretKey()
        );
    }

    @Override
    public void send(PassportManualNotification notification) {
        if (!properties.getMail().isEnabled()) {
            return;
        }
        String queueUrl = properties.getSqs().getQueueUrl();
        Assert.hasText(queueUrl, "app.workflow.passport-manual.sqs.queue-url is required when provider=SQS");

        List<ManualAttachment> attachments = attachmentResolver.resolveAttachments(notification);
        SqsSendHelper.send(
            sqsClient, objectMapper, queueUrl,
            properties.getSqs().getFifoMessageGroupId(),
            notification.dedupeKey(),
            new PassportManualQueueMessage(
                "PASSPORT_MANUAL_DELIVERY",
                notification.passportId(),
                notification.recipientEmail(),
                notification.serialNumber(),
                notification.modelName(),
                notification.message(),
                attachments
            )
        );
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
