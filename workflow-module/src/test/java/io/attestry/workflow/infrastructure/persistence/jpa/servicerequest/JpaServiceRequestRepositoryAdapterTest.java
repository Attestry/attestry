package io.attestry.workflow.infrastructure.persistence.jpa.servicerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import io.attestry.workflow.infrastructure.persistence.jpa.servicerequest.entity.WorkflowServiceRequestJpaEntity;
import io.attestry.workflow.infrastructure.persistence.jpa.servicerequest.mapper.ServiceRequestMapper;
import io.attestry.workflow.infrastructure.persistence.jpa.servicerequest.repository.ServiceRequestJpaRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class JpaServiceRequestRepositoryAdapterTest {

    @Mock ServiceRequestJpaRepository repository;

    private JpaServiceRequestRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaServiceRequestRepositoryAdapter(repository, new ServiceRequestMapper());
    }

    @Test
    void save_duplicateOpenRequestConstraint_translatesToDomainError() {
        ServiceRequest request = ServiceRequest.submit(
            "sr1",
            "p1",
            null,
            "owner1",
            "provider1",
            null,
            "eg1",
            "ONLINE",
            "screen issue",
            null,
            "memo",
            "perm1",
            "owner1",
            Instant.parse("2026-03-12T10:00:00Z"),
            Instant.parse("2026-03-12T10:00:00Z")
        );

        when(repository.save(org.mockito.ArgumentMatchers.any()))
            .thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_workflow_service_requests_open_passport\""
            ));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () -> adapter.save(request));

        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_ALREADY_SUBMITTED, ex.getErrorCode());
    }

    @Test
    void findById_mapsEntityToDomain() {
        WorkflowServiceRequestJpaEntity entity = new WorkflowServiceRequestJpaEntity(
            "sr1",
            "p1",
            "REPAIR",
            "owner1",
            "provider1",
            ServiceRequestStatus.PENDING,
            "desc",
            "ONLINE",
            "symptom",
            null,
            "memo",
            "before-eg",
            null,
            null,
            null,
            "perm1",
            "owner1",
            Instant.parse("2026-03-12T10:00:00Z"),
            null,
            null,
            null,
            null,
            Instant.parse("2026-03-12T10:00:00Z")
        );
        when(repository.findById("sr1")).thenReturn(Optional.of(entity));

        Optional<ServiceRequest> result = adapter.findById("sr1");

        assertTrue(result.isPresent());
        assertEquals("sr1", result.get().serviceRequestId());
        assertEquals("p1", result.get().passportId());
        verify(repository).findById("sr1");
    }
}
