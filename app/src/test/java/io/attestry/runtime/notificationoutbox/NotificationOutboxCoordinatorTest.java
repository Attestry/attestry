package io.attestry.runtime.notificationoutbox;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxCoordinatorTest {

    @Mock private NotificationOutboxBatchProcessor batchProcessor;
    @Mock private NotificationOutboxBacklogMetricsRefresher backlogMetricsRefresher;
    @Mock private NotificationOutboxExecutionContextFactory executionContextFactory;

    @Test
    void publishPending_delegatesToBatchProcessorAndRefreshesMetrics() {
        org.mockito.Mockito.when(executionContextFactory.createProcessingOwner(org.mockito.ArgumentMatchers.any()))
            .thenReturn("notification-publisher-test");
        NotificationOutboxCoordinator coordinator = new NotificationOutboxCoordinator(
            batchProcessor,
            backlogMetricsRefresher,
            executionContextFactory
        );

        coordinator.publishPending();

        verify(batchProcessor).publishPending("notification-publisher-test");
        verify(backlogMetricsRefresher).refresh(org.mockito.ArgumentMatchers.any());
    }
}
