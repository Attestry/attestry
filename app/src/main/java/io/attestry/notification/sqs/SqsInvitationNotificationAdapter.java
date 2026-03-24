package io.attestry.notification.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.notification.InvitationNotificationProperties;
import io.attestry.userauth.application.port.notification.InvitationNotificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
@ConditionalOnProperty(
    prefix = "app.user-auth.invitation.mail",
    name = "provider",
    havingValue = "SQS"
)
public class SqsInvitationNotificationAdapter implements InvitationNotificationPort {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final InvitationNotificationProperties properties;

    public SqsInvitationNotificationAdapter(
        ObjectMapper objectMapper,
        InvitationNotificationProperties properties
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        InvitationNotificationProperties.Sqs sqs = properties.getSqs();
        this.sqsClient = SqsClientFactory.create(
            sqs.getRegion(), sqs.getEndpoint(), sqs.getAccessKey(), sqs.getSecretKey()
        );
    }

    @Override
    public void send(InvitationNotification notification) {
        if (!properties.getMail().isEnabled()) {
            return;
        }
        String queueUrl = properties.getSqs().getQueueUrl();
        Assert.hasText(queueUrl, "app.user-auth.invitation.sqs.queue-url is required when provider=SQS");

        SqsSendHelper.send(
            sqsClient, objectMapper, queueUrl,
            properties.getSqs().getFifoMessageGroupId(),
            notification.dedupeKey(),
            new InvitationQueueMessage(
                "INVITATION",
                notification.invitationId(),
                notification.tenantId(),
                notification.inviteeEmail(),
                String.format(properties.getAcceptUrlTemplate(), notification.invitationId())
            )
        );
    }

    private record InvitationQueueMessage(
        String type,
        String invitationId,
        String tenantId,
        String inviteeEmail,
        String acceptUrl
    ) {
    }
}
