package io.attestry.job.outbox.model;

public enum OutboxAggregateType {
    PRODUCT("PRODUCT", true),
    SHIPMENT("SHIPMENT", true),
    TRANSFER("TRANSFER", true),
    OTHER("OTHER", false);

    private final String dbValue;
    private final boolean projectionFanoutTarget;

    OutboxAggregateType(String dbValue, boolean projectionFanoutTarget) {
        this.dbValue = dbValue;
        this.projectionFanoutTarget = projectionFanoutTarget;
    }

    public String dbValue() {
        return dbValue;
    }

    public boolean isProjectionFanoutTarget() {
        return projectionFanoutTarget;
    }

    public static OutboxAggregateType fromDbValue(String dbValue) {
        for (OutboxAggregateType value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        return OTHER;
    }
}
