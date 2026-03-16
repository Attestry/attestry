package io.attestry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public class AppKafkaProperties {

    private boolean enabled = true;
    private final Topics topics = new Topics();
    private final Outbox outbox = new Outbox();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Topics getTopics() {
        return topics;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public static class Topics {
        private String ledgerOutbox = "ledger.outbox.v1";
        private String productProjection = "product.projection.v1";

        public String getLedgerOutbox() {
            return ledgerOutbox;
        }

        public void setLedgerOutbox(String ledgerOutbox) {
            this.ledgerOutbox = ledgerOutbox;
        }

        public String getProductProjection() {
            return productProjection;
        }

        public void setProductProjection(String productProjection) {
            this.productProjection = productProjection;
        }
    }

    public static class Outbox {
        private int batchSize = 100;
        private int maxRetries = 10;
        private int cleanupRetentionDays = 7;
        private int processingTimeoutSeconds = 300;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getCleanupRetentionDays() {
            return cleanupRetentionDays;
        }

        public void setCleanupRetentionDays(int cleanupRetentionDays) {
            this.cleanupRetentionDays = cleanupRetentionDays;
        }

        public int getProcessingTimeoutSeconds() {
            return processingTimeoutSeconds;
        }

        public void setProcessingTimeoutSeconds(int processingTimeoutSeconds) {
            this.processingTimeoutSeconds = processingTimeoutSeconds;
        }
    }
}
