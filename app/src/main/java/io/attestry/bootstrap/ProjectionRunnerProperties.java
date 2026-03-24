package io.attestry.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class ProjectionRunnerProperties {

    private Workflow workflow = new Workflow();
    private Product product = new Product();

    @Getter
    @Setter
    public static class Workflow {
        private ReadProjection readProjection = new ReadProjection();
    }

    @Getter
    @Setter
    public static class Product {
        private ReadProjection readProjection = new ReadProjection();
    }

    @Getter
    @Setter
    public static class ReadProjection {
        private RunnerToggle backfill = new RunnerToggle();
        private RunnerToggle validation = new RunnerToggle();
    }

    @Getter
    @Setter
    public static class RunnerToggle {
        private boolean enabled = false;
    }
}
