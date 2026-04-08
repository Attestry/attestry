package io.attestry.runtime.ledgeroutbox.repository;

import io.attestry.runtime.ledgeroutbox.model.*;
public final class OutboxJobSql {

    private OutboxJobSql() {
    }

    public static String ledgerAppend() {
        return OutboxEventType.LEDGER_APPEND.dbValue();
    }

    public static String projectionUpdate() {
        return OutboxEventType.PROJECTION_UPDATE.dbValue();
    }

    public static String pending() {
        return OutboxStatus.PENDING.dbValue();
    }

    public static String processing() {
        return OutboxStatus.PROCESSING.dbValue();
    }

    public static String published() {
        return OutboxStatus.PUBLISHED.dbValue();
    }
}
