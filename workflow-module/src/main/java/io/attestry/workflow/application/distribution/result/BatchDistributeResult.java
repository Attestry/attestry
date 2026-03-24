package io.attestry.workflow.application.distribution.result;

import java.util.List;

public record BatchDistributeResult(
    List<Entry> results,
    int totalRequested,
    long totalDistributed
) {
    public record Entry(
        String passportId,
        String distributionId,
        String delegationId,
        String status,
        String error
    ) {
        public static Entry success(String passportId, String distributionId, String delegationId) {
            return new Entry(passportId, distributionId, delegationId, "DISTRIBUTED", null);
        }

        public static Entry failed(String passportId, String error) {
            return new Entry(passportId, null, null, "FAILED", error);
        }

        public boolean isSuccess() {
            return "DISTRIBUTED".equals(status);
        }
    }
}
