package io.attestry.workflow.infrastructure.persistence.jpa.servicerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class JdbcServiceRequestRepositoryAdapterTest {

    @Mock NamedParameterJdbcTemplate jdbcTemplate;
    @Mock JdbcOperations jdbcOperations;

    private JdbcServiceRequestRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JdbcServiceRequestRepositoryAdapter(jdbcTemplate);
        lenient().when(jdbcTemplate.getJdbcOperations()).thenReturn(jdbcOperations);
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

        when(jdbcOperations.update(anyString(), any(Object[].class)))
            .thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_workflow_service_requests_open_passport\""
            ));

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () -> adapter.save(request));

        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_ALREADY_SUBMITTED, ex.getErrorCode());
    }
}
