package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.application.port.BrandAccessValidationPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcBrandAccessValidationAdapter implements BrandAccessValidationPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcBrandAccessValidationAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void assertActiveBrandMembership(String actorUserId, String tenantId, String groupId) {
        Integer membershipCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(1)
                FROM memberships m
                JOIN tenant_groups g ON g.group_id = m.group_id
                JOIN tenants t ON t.tenant_id = m.tenant_id
                WHERE m.user_id = ?
                  AND m.tenant_id = ?
                  AND m.group_id = ?
                  AND m.status = 'ACTIVE'
                  AND g.status = 'ACTIVE'
                  AND g.type = 'BRAND'
                  AND t.status = 'ACTIVE'
            """,
            Integer.class,
            actorUserId,
            tenantId,
            groupId
        );
        if (membershipCount == null || membershipCount == 0) {
            throw new ProductDomainException(ProductErrorCode.MINT_CONTEXT_NOT_FOUND, "Active BRAND membership context is required");
        }
    }
}
