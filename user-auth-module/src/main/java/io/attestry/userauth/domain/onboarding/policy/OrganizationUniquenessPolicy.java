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
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_ORGANIZATION_NAME, "해당 국가에 이미 등록된 브랜드명입니다.");
        }
        String normalizedBizRegNo = normalizeOrNull(bizRegNo);
        if (normalizedBizRegNo != null && repository.existsBrandByBizRegNo(normalizedBizRegNo)) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_BIZ_REG_NO, "이미 등록된 사업자 등록번호입니다.");
        }
    }

    public void assertUniqueRetail(String orgName, String country, String bizRegNo) {
        String normalizedOrgName = normalize(orgName);
        String normalizedCountry = normalize(country);
        if (repository.existsBrandByOrgNameAndCountry(normalizedOrgName, normalizedCountry)
                || repository.existsRetailByTenantAndOrgNameAndCountry(null, normalizedOrgName, normalizedCountry)) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_ORGANIZATION_NAME, "해당 국가에 이미 등록된 판매처명입니다.");
        }
        String normalizedBizRegNo = normalizeOrNull(bizRegNo);
        if (normalizedBizRegNo != null
                && (repository.existsBrandByBizRegNo(normalizedBizRegNo)
                        || repository.existsRetailByTenantAndBizRegNo(null, normalizedBizRegNo))) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_BIZ_REG_NO, "이미 등록된 사업자 등록번호입니다.");
        }
    }

    public void assertUniqueService(String orgName, String country, String bizRegNo) {
        String normalizedOrgName = normalize(orgName);
        String normalizedCountry = normalize(country);
        if (repository.existsBrandByOrgNameAndCountry(normalizedOrgName, normalizedCountry)
                || repository.existsRetailByTenantAndOrgNameAndCountry(null, normalizedOrgName, normalizedCountry)
                || repository.existsServiceByTenantAndOrgNameAndCountry(null, normalizedOrgName, normalizedCountry)) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_ORGANIZATION_NAME, "해당 국가에 이미 등록된 서비스 업체명입니다.");
        }
        String normalizedBizRegNo = normalizeOrNull(bizRegNo);
        if (normalizedBizRegNo != null
                && (repository.existsBrandByBizRegNo(normalizedBizRegNo)
                        || repository.existsRetailByTenantAndBizRegNo(null, normalizedBizRegNo)
                        || repository.existsServiceByTenantAndBizRegNo(null, normalizedBizRegNo))) {
            throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_BIZ_REG_NO, "이미 등록된 사업자 등록번호입니다.");
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
