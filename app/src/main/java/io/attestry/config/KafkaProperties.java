package io.attestry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {

    private boolean enabled = true;
    private String clientId = "attestry";
    private String consumerGroupId = "attestry-ledger-consumer";
    private final Topics topics = new Topics();
    private final Outbox outbox = new Outbox();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getConsumerGroupId() {
        return consumerGroupId;
    }

    public void setConsumerGroupId(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
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
        private long publishIntervalMs = 2000;

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

        public long getPublishIntervalMs() {
            return publishIntervalMs;
        }

        public void setPublishIntervalMs(long publishIntervalMs) {
            this.publishIntervalMs = publishIntervalMs;
        }
    }
}
