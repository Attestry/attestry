package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.RoleAssignmentAuditJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleAssignmentAuditJpaRepository extends JpaRepository<RoleAssignmentAuditJpaEntity, String> {
}
