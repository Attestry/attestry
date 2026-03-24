package io.attestry.notification.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.notification.SignUpVerificationNotificationProperties;
import io.attestry.userauth.application.port.notification.SignUpEmailVerificationNotificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
@ConditionalOnProperty(
    prefix = "app.user-auth.signup-email-verification.mail",
    name = "provider",
    havingValue = "SQS"
)
public class SqsSignUpEmailVerificationNotificationAdapter implements SignUpEmailVerificationNotificationPort {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final SignUpVerificationNotificationProperties properties;

    public SqsSignUpEmailVerificationNotificationAdapter(
        ObjectMapper objectMapper,
        SignUpVerificationNotificationProperties properties
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        SignUpVerificationNotificationProperties.Sqs sqs = properties.getSqs();
        this.sqsClient = SqsClientFactory.create(
            sqs.getRegion(), sqs.getEndpoint(), sqs.getAccessKey(), sqs.getSecretKey()
        );
    }

    @Override
    public void send(SignUpEmailVerificationNotification notification) {
        if (!properties.getMail().isEnabled()) {
            return;
        }
        String queueUrl = properties.getSqs().getQueueUrl();
        Assert.hasText(queueUrl, "app.user-auth.signup-email-verification.sqs.queue-url is required when provider=SQS");

        SqsSendHelper.send(
            sqsClient, objectMapper, queueUrl,
            properties.getSqs().getFifoMessageGroupId(),
            notification.dedupeKey(),
            new SignUpEmailVerificationQueueMessage(
                "SIGNUP_EMAIL_VERIFICATION",
                notification.verificationId(),
                notification.email(),
                notification.code(),
                notification.expiresInSeconds()
            )
        );
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
