package io.attestry.runtime.ledgeroutbox.model;

import java.util.ArrayList;
import java.util.List;

public record OutboxPublishSummary(
    List<String> successEventIds,
    List<PublishAttempt> failedAttempts,
    int totalAttempts
) {

    public static OutboxPublishSummary from(List<PublishAttempt> attempts) {
        List<String> successEventIds = new ArrayList<>();
        List<PublishAttempt> failedAttempts = new ArrayList<>();
        for (PublishAttempt attempt : attempts) {
            if (attempt.error() == null) {
                successEventIds.add(attempt.event().eventId());
            } else {
                failedAttempts.add(attempt);
            }
        }
        return new OutboxPublishSummary(successEventIds, failedAttempts, attempts.size());
    }
}
