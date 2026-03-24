package io.attestry.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.workflow.passport-manual")
public class PassportManualNotificationProperties {

    private Mail mail = new Mail();
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
        private String fifoMessageGroupId = "passport-manual-notification";
    }
}
