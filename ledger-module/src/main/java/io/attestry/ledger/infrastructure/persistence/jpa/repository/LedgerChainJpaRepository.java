package io.attestry.ledger.infrastructure.persistence.jpa.repository;

import io.attestry.ledger.infrastructure.persistence.jpa.entity.LedgerChainJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerChainJpaRepository extends JpaRepository<LedgerChainJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LedgerChainJpaEntity c where c.passportId = :passportId")
    Optional<LedgerChainJpaEntity> findByPassportIdForUpdate(@Param("passportId") String passportId);
}
