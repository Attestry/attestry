package io.attestry.workflow.domain.servicerequest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ServiceRequestTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    @Test
    void submit_setsFieldsCorrectly() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "tenant1", "group1", "Fix screen",
            "eg1", "perm1", "provider1", NOW, NOW
        );

        assertEquals("sr1", request.serviceRequestId());
        assertEquals("p1", request.passportId());
        assertEquals("REPAIR", request.serviceType());
        assertEquals("owner1", request.ownerUserId());
        assertEquals("tenant1", request.providerTenantId());
        assertEquals("group1", request.providerGroupId());
        assertEquals(ServiceRequestStatus.SUBMITTED, request.status());
        assertEquals("Fix screen", request.description());
        assertEquals("eg1", request.beforeEvidenceGroupId());
        assertNull(request.afterEvidenceGroupId());
        assertEquals("perm1", request.permissionId());
        assertEquals("provider1", request.submittedByUserId());
        assertEquals(NOW, request.submittedAt());
        assertNull(request.completedAt());
        assertNull(request.cancelledAt());
    }

    @Test
    void submit_requiresServiceRequestId() {
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            ServiceRequest.submit(null, "p1", "REPAIR", "owner1", "t1", "g1", null, null, null, "provider1", NOW, NOW)
        );
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void submit_requiresSubmittedByUserId() {
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            ServiceRequest.submit("sr1", "p1", "REPAIR", "owner1", "t1", "g1", null, null, null, null, NOW, NOW)
        );
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void complete_fromSubmitted_succeeds() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "tenant1", "group1", "desc", "eg1", "perm1", "provider1", NOW, NOW
        );

        Instant completedAt = Instant.parse("2026-03-01T12:00:00Z");
        ServiceRequest completed = request.complete("provider1", "afterEg1", completedAt);

        assertEquals(ServiceRequestStatus.COMPLETED, completed.status());
        assertEquals("provider1", completed.completedByUserId());
        assertEquals("afterEg1", completed.afterEvidenceGroupId());
        assertEquals(completedAt, completed.completedAt());
        assertEquals("provider1", completed.submittedByUserId());
        assertNull(completed.cancelledAt());
    }

    @Test
    void complete_whenNotSubmitted_throws() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "tenant1", "group1", "desc", null, null, "provider1", NOW, NOW
        );
        ServiceRequest cancelled = request.cancel("reason", NOW);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            cancelled.complete("provider1", "afterEg1", NOW)
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void cancel_fromSubmitted_succeeds() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "tenant1", "group1", "desc", null, null, "provider1", NOW, NOW
        );

        Instant cancelledAt = Instant.parse("2026-03-01T11:00:00Z");
        ServiceRequest cancelled = request.cancel("No longer needed", cancelledAt);

        assertEquals(ServiceRequestStatus.CANCELLED, cancelled.status());
        assertEquals("No longer needed", cancelled.cancelReason());
        assertEquals(cancelledAt, cancelled.cancelledAt());
        assertEquals("provider1", cancelled.submittedByUserId());
        assertNull(cancelled.completedAt());
    }

    @Test
    void cancel_whenNotSubmitted_throws() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "tenant1", "group1", "desc", null, null, "provider1", NOW, NOW
        );
        ServiceRequest completed = request.complete("provider1", "afterEg1", NOW);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            completed.cancel("reason", NOW)
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, ex.getErrorCode());
    }
}
