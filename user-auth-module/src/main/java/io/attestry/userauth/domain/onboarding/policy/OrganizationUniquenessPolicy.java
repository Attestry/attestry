package io.attestry.userauth.domain.onboarding.policy;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.onboarding.repository.OrganizationApplicationRepository;
import org.springframework.stereotype.Component;

@Component
public class OrganizationUniquenessPolicy {

    private final OrganizationApplicationRepository repository;

    public OrganizationUniquenessPolicy(OrganizationApplicationRepository repository) {
        this.repository = repository;
    }

    public void assertUniqueBrand(String orgName, String bizRegNo) {
        String normalizedOrgName = normalize(orgName);
        if (repository.existsBrandByOrgName(normalizedOrgName)) {
            throw new DomainException(ErrorCode.DUPLICATE_ORGANIZATION_NAME, "Brand name already exists");
        }
        String normalizedBizRegNo = normalizeOrNull(bizRegNo);
        if (normalizedBizRegNo != null && repository.existsBrandByBizRegNo(normalizedBizRegNo)) {
            throw new DomainException(ErrorCode.DUPLICATE_BIZ_REG_NO, "Business registration number already exists");
        }
    }

    public void assertUniqueRetail(String orgName, String bizRegNo) {
        String normalizedOrgName = normalize(orgName);
        if (repository.existsBrandByOrgName(normalizedOrgName)
                || repository.existsRetailByTenantAndOrgName(null, normalizedOrgName)) {
            throw new DomainException(ErrorCode.DUPLICATE_ORGANIZATION_NAME, "Retail name already exists");
        }
        String normalizedBizRegNo = normalizeOrNull(bizRegNo);
        if (normalizedBizRegNo != null
                && (repository.existsBrandByBizRegNo(normalizedBizRegNo)
                        || repository.existsRetailByTenantAndBizRegNo(null, normalizedBizRegNo))) {
            throw new DomainException(ErrorCode.DUPLICATE_BIZ_REG_NO, "Business registration number already exists");
        }
    }

    public void assertUniqueService(String orgName, String bizRegNo) {
        String normalizedOrgName = normalize(orgName);
        if (repository.existsBrandByOrgName(normalizedOrgName)
                || repository.existsRetailByTenantAndOrgName(null, normalizedOrgName)
                || repository.existsServiceByTenantAndOrgName(null, normalizedOrgName)) {
            throw new DomainException(ErrorCode.DUPLICATE_ORGANIZATION_NAME, "Service name already exists");
        }
        String normalizedBizRegNo = normalizeOrNull(bizRegNo);
        if (normalizedBizRegNo != null
                && (repository.existsBrandByBizRegNo(normalizedBizRegNo)
                        || repository.existsRetailByTenantAndBizRegNo(null, normalizedBizRegNo)
                        || repository.existsServiceByTenantAndBizRegNo(null, normalizedBizRegNo))) {
            throw new DomainException(ErrorCode.DUPLICATE_BIZ_REG_NO, "Business registration number already exists");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOrNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
