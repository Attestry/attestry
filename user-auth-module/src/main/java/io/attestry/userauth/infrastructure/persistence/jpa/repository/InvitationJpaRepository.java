package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.InvitationJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationJpaRepository extends JpaRepository<InvitationJpaEntity, String> {
    List<InvitationJpaEntity> findByTenantId(String tenantId);
}
