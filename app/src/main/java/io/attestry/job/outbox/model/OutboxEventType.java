package io.attestry.job.outbox.model;

public enum OutboxEventType {
    LEDGER_APPEND("LEDGER_APPEND"),
    PROJECTION_UPDATE("PROJECTION_UPDATE");

    private final String dbValue;

    OutboxEventType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static OutboxEventType fromDbValue(String dbValue) {
        for (OutboxEventType value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown outbox event type: " + dbValue);
    }
}
