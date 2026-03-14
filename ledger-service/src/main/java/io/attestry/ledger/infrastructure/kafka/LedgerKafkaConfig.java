package io.attestry.ledger.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(LedgerKafkaProperties.class)
@ConditionalOnProperty(prefix = "app.ledger.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerKafkaConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.ledger.kafka.topic-auto-create", name = "enabled", havingValue = "true")
    public NewTopic ledgerOutboxTopic(LedgerKafkaProperties properties) {
        return TopicBuilder.name(properties.getTopics().getLedgerOutbox())
            .partitions(properties.getTopics().getPartitions())
            .replicas(properties.getTopics().getReplicas())
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ledger.kafka.topic-auto-create", name = "enabled", havingValue = "true")
    public NewTopic ledgerDlqTopic(LedgerKafkaProperties properties) {
        return TopicBuilder.name(properties.getTopics().getLedgerDlq())
            .partitions(properties.getTopics().getPartitions())
            .replicas(properties.getTopics().getReplicas())
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ledger.kafka.topic-auto-create", name = "enabled", havingValue = "true")
    public NewTopic productProjectionTopic(LedgerKafkaProperties properties) {
        return TopicBuilder.name(properties.getTopics().getProductProjection())
            .partitions(properties.getTopics().getPartitions())
            .replicas(properties.getTopics().getReplicas())
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ledger.kafka.topic-auto-create", name = "enabled", havingValue = "true")
    public NewTopic productProjectionDlqTopic(LedgerKafkaProperties properties) {
        return TopicBuilder.name(properties.getTopics().getProductProjectionDlq())
            .partitions(properties.getTopics().getPartitions())
            .replicas(properties.getTopics().getReplicas())
            .build();
    }
}
