package io.attestry.workflow.infrastructure.config;

import io.attestry.workflow.application.EvidenceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EvidenceProperties.class)
public class WorkflowPropertiesConfig {
}
