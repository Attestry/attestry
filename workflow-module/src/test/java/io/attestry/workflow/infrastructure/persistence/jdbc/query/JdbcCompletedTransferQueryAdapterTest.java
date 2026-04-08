package io.attestry.workflow.infrastructure.persistence.jdbc.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.workflow.application.port.transfer.CompletedTransferQueryPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class JdbcCompletedTransferQueryAdapterTest {

    @Mock NamedParameterJdbcTemplate jdbcTemplate;
    @Mock JdbcOperations jdbcOperations;

    private JdbcCompletedTransferQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JdbcCompletedTransferQueryAdapter(jdbcTemplate);
        when(jdbcTemplate.getJdbcOperations()).thenReturn(jdbcOperations);
    }

    @Test
    void findCompletedB2CByTenantId_withSourceTenantFilter_returnsPagedRows() {
        when(jdbcOperations.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenReturn(1L);
        when(jdbcOperations.query(anyString(), anyRowMapper(), any(Object[].class)))
            .thenReturn(List.of(new CompletedTransferQueryPort.CompletedTransferRow(
                "t1",
                "p1",
                "brand-1",
                "SN-1",
                "Model-1",
                "ACTIVE",
                "owner-1",
                "QR",
                Instant.parse("2026-03-12T10:00:00Z")
            )));

        CompletedTransferQueryPort.PagedResult result = adapter.findCompletedB2CByTenantId("tenant-1", "brand-1", 0, 20);

        assertEquals(1L, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals("t1", result.content().getFirst().transferId());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcOperations).queryForObject(sqlCaptor.capture(), eq(Long.class), any(Object[].class));
        assertTrue(sqlCaptor.getValue().contains("wpsp.tenant_id = ?"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    @SuppressWarnings("unchecked")
    private static <T> RowMapper<T> anyRowMapper() {
        return (RowMapper<T>) any(RowMapper.class);
    }
}
