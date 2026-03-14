package io.attestry.ledger.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ledger.kafka")
public class LedgerKafkaProperties {

    private boolean enabled = true;
    private String consumerGroupId = "attestry-ledger-consumer";
    private int listenerConcurrency = 1;
    private final Topics topics = new Topics();
    private final Outbox outbox = new Outbox();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConsumerGroupId() {
        return consumerGroupId;
    }

    public void setConsumerGroupId(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public int getListenerConcurrency() {
        return listenerConcurrency;
    }

    public void setListenerConcurrency(int listenerConcurrency) {
        this.listenerConcurrency = listenerConcurrency;
    }

    public Topics getTopics() {
        return topics;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public static class Topics {
        private String ledgerOutbox = "ledger.outbox.v1";
        private String ledgerDlq = "ledger.outbox.v1.dlq";
        private String productProjection = "product.projection.v1";
        private String productProjectionDlq = "product.projection.v1.dlq";
        private int partitions = 3;
        private short replicas = 1;

        public String getLedgerOutbox() {
            return ledgerOutbox;
        }

        public void setLedgerOutbox(String ledgerOutbox) {
            this.ledgerOutbox = ledgerOutbox;
        }

        public String getLedgerDlq() {
            return ledgerDlq;
        }

        public void setLedgerDlq(String ledgerDlq) {
            this.ledgerDlq = ledgerDlq;
        }

        public String getProductProjection() {
            return productProjection;
        }

        public void setProductProjection(String productProjection) {
            this.productProjection = productProjection;
        }

        public String getProductProjectionDlq() {
            return productProjectionDlq;
        }

        public void setProductProjectionDlq(String productProjectionDlq) {
            this.productProjectionDlq = productProjectionDlq;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        public short getReplicas() {
            return replicas;
        }

        public void setReplicas(short replicas) {
            this.replicas = replicas;
        }
    }

    public static class Outbox {
        private int batchSize = 100;
        private int maxRetries = 5;

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
    }
}
