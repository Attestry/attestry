package io.attestry.bootstrap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    BootstrapProperties.class,
    ProjectionRunnerProperties.class
})
public class BootstrapPropertiesConfig {
}
