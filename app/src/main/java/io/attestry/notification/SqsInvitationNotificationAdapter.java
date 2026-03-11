package io.attestry.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.userauth.application.port.notification.InvitationNotificationPort;
import java.net.URI;
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
    prefix = "app.user-auth.invitation.mail",
    name = "provider",
    havingValue = "SQS"
)
public class SqsInvitationNotificationAdapter implements InvitationNotificationPort {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String queueUrl;
    private final String acceptUrlTemplate;
    private final String fifoMessageGroupId;

    public SqsInvitationNotificationAdapter(
        ObjectMapper objectMapper,
        @Value("${app.user-auth.invitation.mail.enabled:true}") boolean enabled,
        @Value("${app.user-auth.invitation.accept-url-template:http://localhost:8080/invitations/%s/accept}") String acceptUrlTemplate,
        @Value("${app.user-auth.invitation.sqs.queue-url:}") String queueUrl,
        @Value("${app.user-auth.invitation.sqs.region:ap-northeast-2}") String region,
        @Value("${app.user-auth.invitation.sqs.endpoint:}") String endpoint,
        @Value("${app.user-auth.invitation.sqs.access-key:}") String accessKey,
        @Value("${app.user-auth.invitation.sqs.secret-key:}") String secretKey,
        @Value("${app.user-auth.invitation.sqs.fifo-message-group-id:invitation-notification}") String fifoMessageGroupId
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.queueUrl = queueUrl;
        this.acceptUrlTemplate = acceptUrlTemplate;
        this.fifoMessageGroupId = fifoMessageGroupId;

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
    public void send(InvitationNotification notification) {
        if (!enabled) {
            return;
        }
        Assert.hasText(queueUrl, "app.user-auth.invitation.sqs.queue-url is required when provider=SQS");

        String body = serialize(notification);
        SendMessageRequest.Builder request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(body);

        if (queueUrl.endsWith(".fifo")) {
            request.messageGroupId(fifoMessageGroupId);
            request.messageDeduplicationId(notification.invitationId());
        }

        sqsClient.sendMessage(request.build());
    }

    private String serialize(InvitationNotification notification) {
        try {
            return objectMapper.writeValueAsString(new InvitationQueueMessage(
                "INVITATION",
                notification.invitationId(),
                notification.tenantId(),
                notification.inviteeEmail(),
                String.format(acceptUrlTemplate, notification.invitationId())
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize invitation notification for SQS", ex);
        }
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
