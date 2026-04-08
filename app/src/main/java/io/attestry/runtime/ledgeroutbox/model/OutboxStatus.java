package io.attestry.runtime.ledgeroutbox.model;

public enum OutboxStatus {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    PUBLISHED("PUBLISHED"),
    FAILED("FAILED");

    private final String dbValue;

    OutboxStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }
}
