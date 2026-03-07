package io.attestry.workflow.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.PartnerLinkJpaRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class JpaPartnerLinkRepositoryAdapterTest {

    @Mock PartnerLinkJpaRepository repository;

    @Test
    void save_duplicateStatusConstraint_throwsDomainErrorCode() {
        JpaPartnerLinkRepositoryAdapter adapter = new JpaPartnerLinkRepositoryAdapter(repository);
        PartnerLink partnerLink = new PartnerLink(
            "pl-1",
            "tenant-a",
            "tenant-b",
            PartnerType.RETAIL,
            PartnerLinkStatus.PENDING,
            "user-1",
            Instant.parse("2026-03-07T00:00:00Z"),
            null,
            null,
            null,
            null,
            null
        );

        when(repository.findById("pl-1")).thenReturn(Optional.empty());
        when(repository.save(any())).thenThrow(
            new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_partner_links_by_status\""
            )
        );

        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () -> adapter.save(partnerLink));
        assertEquals(WorkflowErrorCode.PARTNER_LINK_DUPLICATE_STATUS, ex.getErrorCode());
    }
}
