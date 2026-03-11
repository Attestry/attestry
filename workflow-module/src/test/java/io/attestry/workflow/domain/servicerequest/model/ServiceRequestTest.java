package io.attestry.workflow.domain.servicerequest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            "tenant1", "Fix screen",
            "eg1", "ONLINE", "화면 불량", null, "010-0000-0000 / 평일 오후", "perm1", "owner1", NOW, NOW
        );

        assertEquals("sr1", request.serviceRequestId());
        assertEquals("p1", request.passportId());
        assertEquals("REPAIR", request.serviceType());
        assertEquals("owner1", request.ownerUserId());
        assertEquals("tenant1", request.providerTenantId());
        assertEquals(ServiceRequestStatus.PENDING, request.status());
        assertEquals("Fix screen", request.description());
        assertEquals("eg1", request.beforeEvidenceGroupId());
        assertNull(request.afterEvidenceGroupId());
        assertEquals("perm1", request.permissionId());
        assertEquals("owner1", request.submittedByUserId());
        assertEquals(NOW, request.submittedAt());
        assertNull(request.completedAt());
        assertNull(request.cancelledAt());
    }

    @Test
    void submit_requiresServiceRequestId() {
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            ServiceRequest.submit(null, "p1", "REPAIR", "owner1", "t1", null, "eg1", "ONLINE", "화면 불량", null, "연락처", "perm1", "owner1", NOW, NOW)
        );
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void submit_requiresSubmittedByUserId() {
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            ServiceRequest.submit("sr1", "p1", "REPAIR", "owner1", "t1", null, "eg1", "ONLINE", "화면 불량", null, "연락처", null, null, NOW, NOW)
        );
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void accept_fromPending_succeeds() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "tenant1", "desc", "eg1", "ONLINE", "화면 불량", null, "연락처", "perm1", "owner1", NOW, NOW
        );

        ServiceRequest accepted = request.accept("REPAIR", "desc", NOW);

        assertEquals(ServiceRequestStatus.ACCEPTED, accepted.status());
    }

    @Test
    void complete_fromAccepted_succeeds() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "tenant1", "desc", "eg1", "ONLINE", "화면 불량", null, "연락처", "perm1", "owner1", NOW, NOW
        ).accept("REPAIR", "desc", NOW);

        Instant completedAt = Instant.parse("2026-03-01T12:00:00Z");
        ServiceRequest completed = request.complete("provider1", "afterEg1", "수리 완료", "메모", completedAt);

        assertEquals(ServiceRequestStatus.COMPLETED, completed.status());
        assertEquals("provider1", completed.completedByUserId());
        assertEquals("afterEg1", completed.afterEvidenceGroupId());
        assertEquals(completedAt, completed.completedAt());
        assertNull(completed.cancelledAt());
    }

    @Test
    void complete_whenNotAccepted_throws() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "tenant1", "desc", "eg1", "ONLINE", "화면 불량", null, "연락처", null, "owner1", NOW, NOW
        );

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            request.complete("provider1", "afterEg1", "수리 완료", "메모", NOW)
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void cancel_fromPending_succeeds() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "tenant1", "desc", "eg1", "ONLINE", "화면 불량", null, "연락처", null, "owner1", NOW, NOW
        );

        Instant cancelledAt = Instant.parse("2026-03-01T11:00:00Z");
        ServiceRequest cancelled = request.cancel("No longer needed", cancelledAt);

        assertEquals(ServiceRequestStatus.CANCELLED, cancelled.status());
        assertEquals("No longer needed", cancelled.cancelReason());
        assertEquals(cancelledAt, cancelled.cancelledAt());
        assertNull(cancelled.completedAt());
    }

    @Test
    void reject_fromPending_succeeds() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "tenant1", "desc", "eg1", "ONLINE", "화면 불량", null, "연락처", null, "owner1", NOW, NOW
        );

        ServiceRequest rejected = request.reject("cannot process", NOW);
        assertEquals(ServiceRequestStatus.REJECTED, rejected.status());
    }

    @Test
    void cancel_whenNotCancellable_throws() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1", "p1", "REPAIR", "owner1",
            "tenant1", "desc", "eg1", "ONLINE", "화면 불량", null, "연락처", null, "owner1", NOW, NOW
        ).reject("reason", NOW);

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            request.cancel("reason", NOW)
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE, ex.getErrorCode());
    }
}
