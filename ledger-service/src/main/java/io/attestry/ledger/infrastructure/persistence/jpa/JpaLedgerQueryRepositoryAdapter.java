package io.attestry.ledger.infrastructure.persistence.jpa;

import io.attestry.ledger.application.port.LedgerQueryRepositoryPort;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.infrastructure.persistence.jpa.mapper.LedgerEntryMapper;
import io.attestry.ledger.infrastructure.persistence.jpa.repository.LedgerChainJpaRepository;
import io.attestry.ledger.infrastructure.persistence.jpa.repository.LedgerEntryJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLedgerQueryRepositoryAdapter implements LedgerQueryRepositoryPort {

    private final LedgerEntryJpaRepository repository;
    private final LedgerChainJpaRepository chainRepository;
    private final LedgerEntryMapper mapper;

    public JpaLedgerQueryRepositoryAdapter(
        LedgerEntryJpaRepository repository,
        LedgerChainJpaRepository chainRepository,
        LedgerEntryMapper mapper
    ) {
        this.repository = repository;
        this.chainRepository = chainRepository;
        this.mapper = mapper;
    }

    @Override
    public List<LedgerEntry> findByPassportIdOrderBySeqAsc(String passportId) {
        return repository.findByPassportIdOrderBySeqAsc(passportId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<LedgerEntry> findByPassportIdAndLedgerId(String passportId, String ledgerId) {
        return repository.findByPassportIdAndLedgerId(passportId, ledgerId)
            .map(mapper::toDomain);
    }

    @Override
    public List<String> findAllPassportIds() {
        return chainRepository.findAllPassportIds();
    }
}
