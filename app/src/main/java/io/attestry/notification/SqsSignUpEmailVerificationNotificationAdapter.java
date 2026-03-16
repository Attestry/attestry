package io.attestry.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.userauth.application.port.notification.SignUpEmailVerificationNotificationPort;
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
    prefix = "app.user-auth.signup-email-verification.mail",
    name = "provider",
    havingValue = "SQS"
)
public class SqsSignUpEmailVerificationNotificationAdapter implements SignUpEmailVerificationNotificationPort {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String queueUrl;
    private final String fifoMessageGroupId;

    public SqsSignUpEmailVerificationNotificationAdapter(
        ObjectMapper objectMapper,
        @Value("${app.user-auth.signup-email-verification.mail.enabled:true}") boolean enabled,
        @Value("${app.user-auth.signup-email-verification.sqs.queue-url:}") String queueUrl,
        @Value("${app.user-auth.signup-email-verification.sqs.region:ap-northeast-2}") String region,
        @Value("${app.user-auth.signup-email-verification.sqs.endpoint:}") String endpoint,
        @Value("${app.user-auth.signup-email-verification.sqs.access-key:}") String accessKey,
        @Value("${app.user-auth.signup-email-verification.sqs.secret-key:}") String secretKey,
        @Value("${app.user-auth.signup-email-verification.sqs.fifo-message-group-id:signup-email-verification}") String fifoMessageGroupId
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.queueUrl = queueUrl;
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
    public void send(SignUpEmailVerificationNotification notification) {
        if (!enabled) {
            return;
        }
        Assert.hasText(queueUrl, "app.user-auth.signup-email-verification.sqs.queue-url is required when provider=SQS");

        String body = serialize(notification);
        SendMessageRequest.Builder request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(body);

        if (queueUrl.endsWith(".fifo")) {
            request.messageGroupId(fifoMessageGroupId);
            request.messageDeduplicationId(notification.verificationId());
        }

        sqsClient.sendMessage(request.build());
    }

    private String serialize(SignUpEmailVerificationNotification notification) {
        try {
            return objectMapper.writeValueAsString(new SignUpEmailVerificationQueueMessage(
                "SIGNUP_EMAIL_VERIFICATION",
                notification.verificationId(),
                notification.email(),
                notification.code(),
                notification.expiresInSeconds()
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize signup email verification notification", ex);
        }
    }

    private record SignUpEmailVerificationQueueMessage(
        String type,
        String verificationId,
        String email,
        String code,
        long expiresInSeconds
    ) {
    }
}
