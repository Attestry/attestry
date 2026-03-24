package io.attestry.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LedgerOutboxExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService ledgerOutboxPublishExecutor(AppKafkaProperties kafkaProperties) {
        int configuredParallelism = kafkaProperties.getOutbox().getPublishParallelism();
        int parallelism = configuredParallelism > 0
            ? configuredParallelism
            : Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(Math.max(1, parallelism), new LedgerOutboxThreadFactory());
    }

    private static final class LedgerOutboxThreadFactory implements ThreadFactory {
        private int sequence = 0;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("ledger-outbox-publish-" + sequence++);
            thread.setDaemon(false);
            return thread;
        }
    }
}
