package io.attestry.userauth.domain.onboarding.policy;

import io.attestry.userauth.application.port.onboarding.OrganizationApplicationRepositoryPort;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationUniquenessPolicy {

    private final OrganizationApplicationRepositoryPort repository;

    public void assertUniqueBrand(String orgName, String country, String bizRegNo) {
        String normalizedOrgName = normalize(orgName);
        String normalizedCountry = normalize(country);
        if (repository.existsBrandByOrgNameAndCountry(normalizedOrgName, normalizedCountry)) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_ORGANIZATION_NAME, "Brand name already exists in this country");
        }
        String normalizedBizRegNo = normalizeOrNull(bizRegNo);
        if (normalizedBizRegNo != null && repository.existsBrandByBizRegNo(normalizedBizRegNo)) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_BIZ_REG_NO, "Business registration number already exists");
        }
    }

    public void assertUniqueRetail(String orgName, String country, String bizRegNo) {
        String normalizedOrgName = normalize(orgName);
        String normalizedCountry = normalize(country);
        if (repository.existsBrandByOrgNameAndCountry(normalizedOrgName, normalizedCountry)
                || repository.existsRetailByTenantAndOrgNameAndCountry(null, normalizedOrgName, normalizedCountry)) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_ORGANIZATION_NAME, "Retail name already exists in this country");
        }
        String normalizedBizRegNo = normalizeOrNull(bizRegNo);
        if (normalizedBizRegNo != null
                && (repository.existsBrandByBizRegNo(normalizedBizRegNo)
                        || repository.existsRetailByTenantAndBizRegNo(null, normalizedBizRegNo))) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_BIZ_REG_NO, "Business registration number already exists");
        }
    }

    public void assertUniqueService(String orgName, String country, String bizRegNo) {
        String normalizedOrgName = normalize(orgName);
        String normalizedCountry = normalize(country);
        if (repository.existsBrandByOrgNameAndCountry(normalizedOrgName, normalizedCountry)
                || repository.existsRetailByTenantAndOrgNameAndCountry(null, normalizedOrgName, normalizedCountry)
                || repository.existsServiceByTenantAndOrgNameAndCountry(null, normalizedOrgName, normalizedCountry)) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_ORGANIZATION_NAME, "Service name already exists in this country");
        }
        String normalizedBizRegNo = normalizeOrNull(bizRegNo);
        if (normalizedBizRegNo != null
                && (repository.existsBrandByBizRegNo(normalizedBizRegNo)
                        || repository.existsRetailByTenantAndBizRegNo(null, normalizedBizRegNo)
                        || repository.existsServiceByTenantAndBizRegNo(null, normalizedBizRegNo))) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_BIZ_REG_NO, "Business registration number already exists");
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
