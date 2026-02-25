package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.ApprovalRequestJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestJpaRepository extends JpaRepository<ApprovalRequestJpaEntity, String> {
    List<ApprovalRequestJpaEntity> findByTenantId(String tenantId);
}
