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
            .partitions(6)
            .replicas(1)
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ledger.kafka.topic-auto-create", name = "enabled", havingValue = "true")
    public NewTopic ledgerDlqTopic(LedgerKafkaProperties properties) {
        return TopicBuilder.name(properties.getTopics().getLedgerDlq())
            .partitions(1)
            .replicas(1)
            .build();
    }
}
