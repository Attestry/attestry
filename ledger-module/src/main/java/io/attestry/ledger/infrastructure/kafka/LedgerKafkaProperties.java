package io.attestry.ledger.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ledger.kafka")
public class LedgerKafkaProperties {

    private boolean enabled = true;
    private String consumerGroupId = "attestry-ledger-consumer";
    private int listenerConcurrency = 1;
    private final Topics topics = new Topics();

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

    public static class Topics {
        private String ledgerOutbox = "ledger.outbox.v1";
        private String ledgerDlq = "ledger.outbox.v1.dlq";

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
    }
}
