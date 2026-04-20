package io.attestry.workflow.application.delegation.command;

import java.util.List;

public record BatchDelegationResult(
    List<Entry> results,
    int totalRequested,
    long totalGranted
) {

    public record Entry(
        String passportId,
        String delegationId,
        String status,
        String error
    ) {
        public static Entry granted(String passportId, String delegationId) {
            return new Entry(passportId, delegationId, "GRANTED", null);
        }

        public static Entry failed(String passportId, String error) {
            return new Entry(passportId, null, "FAILED", error);
        }

        public boolean isGranted() {
            return "GRANTED".equals(status);
        }
    }
}
