package io.attestry.userauth.infrastructure.persistence.jpa.mapper;

import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OrganizationApplicationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OrganizationApplicationMapper {

    public OrganizationApplication toDomain(OrganizationApplicationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return OrganizationApplication.reconstitute(
            entity.getApplicationId(),
            entity.getType(),
            entity.getApplicantUserId(),
            entity.getTenantId(),
            entity.getOrgName(),
            entity.getCountry(),
            entity.getAddress(),
            entity.getBizRegNo(),
            entity.getEvidenceBundleId(),
            entity.getStatus(),
            entity.getReviewedByAdminId(),
            entity.getReviewedAt(),
            entity.getRejectReason()
        );
    }

    public OrganizationApplicationJpaEntity toEntity(OrganizationApplication domain, long rowVersion) {
        if (domain == null) {
            return null;
        }
        return new OrganizationApplicationJpaEntity(
            domain.applicationId(),
            domain.type(),
            domain.applicantUserId(),
            domain.tenantId(),
            domain.orgName(),
            domain.country(),
            domain.address(),
            domain.bizRegNo(),
            domain.evidenceBundleId(),
            domain.status(),
            domain.reviewedByAdminId(),
            domain.reviewedAt(),
            domain.rejectReason(),
            rowVersion
        );
    }
}
