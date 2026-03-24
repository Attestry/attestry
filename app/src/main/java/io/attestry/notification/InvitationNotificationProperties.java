package io.attestry.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.user-auth.invitation")
public class InvitationNotificationProperties {

    private Mail mail = new Mail();
    private String acceptUrlTemplate = "http://localhost:8080/invitations/%s/accept";
    private Sqs sqs = new Sqs();

    @Getter
    @Setter
    public static class Mail {
        private boolean enabled = true;
        private String from = "no-reply@attestry.local";
    }

    @Getter
    @Setter
    public static class Sqs {
        private String queueUrl = "";
        private String region = "ap-northeast-2";
        private String endpoint = "";
        private String accessKey = "";
        private String secretKey = "";
        private String fifoMessageGroupId = "invitation-notification";
    }
}
