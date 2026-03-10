package io.attestry.userauth.infrastructure.persistence.jpa.repository.adapter;

import io.attestry.userauth.application.port.MembershipProjectionPort;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipPermissionQueryJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.RoleJpaRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class JpaMembershipProjectionAdapter implements MembershipProjectionPort {

    private final MembershipPermissionQueryJpaRepository queryRepository;
    private final RoleJpaRepository roleRepository;

    @Override
    public Set<String> findPermissionCodesByMembershipId(String membershipId) {
        return new LinkedHashSet<>(queryRepository.findPermissionCodesByMembershipId(membershipId));
    }

    @Override
    public Set<String> findPermissionCodesByGlobalRoleCode(String roleCode) {
        return new LinkedHashSet<>(queryRepository.findPermissionCodesByGlobalRoleCode(roleCode));
    }

    @Override
    public Set<String> findRoleCodesByMembershipId(String membershipId) {
        return new LinkedHashSet<>(queryRepository.findRoleCodesByMembershipId(membershipId));
    }

    @Override
    public Set<String> findGlobalEnabledRoleCodes() {
        return roleRepository.findByTenantIdIsNullAndEnabledTrue().stream()
                .map(role -> role.getCode().trim().toUpperCase(java.util.Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
