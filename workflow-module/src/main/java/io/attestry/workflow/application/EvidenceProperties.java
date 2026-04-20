package io.attestry.workflow.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.workflow.evidence")
public class EvidenceProperties {
    private Duration presignTtl = Duration.ofMinutes(15);
    private Duration downloadTtl = Duration.ofMinutes(30);

    public Duration getPresignTtl() { return presignTtl; }
    public void setPresignTtl(Duration presignTtl) { this.presignTtl = presignTtl; }
    public Duration getDownloadTtl() { return downloadTtl; }
    public void setDownloadTtl(Duration downloadTtl) { this.downloadTtl = downloadTtl; }
}
