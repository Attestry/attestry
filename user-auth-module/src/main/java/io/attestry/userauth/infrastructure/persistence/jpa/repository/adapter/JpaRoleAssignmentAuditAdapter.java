package io.attestry.userauth.infrastructure.persistence.jpa.repository.adapter;

import io.attestry.userauth.application.port.RoleAssignmentAuditPort;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.RoleAssignmentAuditJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.RoleAssignmentAuditJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRoleAssignmentAuditAdapter implements RoleAssignmentAuditPort {

    private final RoleAssignmentAuditJpaRepository repository;

    public JpaRoleAssignmentAuditAdapter(RoleAssignmentAuditJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void log(
        String actorUserId,
        String actorTenantId,
        String targetMembershipId,
        String beforeRole,
        String afterRole,
        String decisionSource,
        boolean allowed,
        String reasonCode,
        Instant requestedAt,
        Instant decidedAt
    ) {
        repository.save(new RoleAssignmentAuditJpaEntity(
            UUID.randomUUID().toString(),
            actorUserId,
            actorTenantId,
            targetMembershipId,
            beforeRole,
            afterRole,
            decisionSource,
            allowed,
            reasonCode,
            requestedAt,
            decidedAt
        ));
    }
}
