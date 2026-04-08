package io.attestry.runtime.workflowprojection;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.workflow.read-projection.kafka")
public class WorkflowReadProjectionKafkaProperties {

    private String sourceTopic = "product.projection.v1";
    private String dlqTopic = "product.projection.v1.dlq";

    public String getSourceTopic() {
        return sourceTopic;
    }

    public void setSourceTopic(String sourceTopic) {
        this.sourceTopic = sourceTopic;
    }

    public String getDlqTopic() {
        return dlqTopic;
    }

    public void setDlqTopic(String dlqTopic) {
        this.dlqTopic = dlqTopic;
    }
}
