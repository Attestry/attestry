package io.attestry.bootstrap;

import io.attestry.userauth.application.port.auth.PasswordHasherPort;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlatformAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrap.class);

    private static final String DEFAULT_USER_ID = "00000000-0000-0000-0000-000000000101";
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000201";
    private static final String DEFAULT_MEMBERSHIP_ID = "00000000-0000-0000-0000-000000000401";
    private static final String DEFAULT_ASSIGNMENT_ID = "00000000-0000-0000-0000-000000000501";
    private static final String PLATFORM_SUPER_ADMIN_ROLE_ID = "role-platform-super-admin";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordHasherPort passwordHasher;
    private final Clock clock;
    private final boolean enabled;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminPhone;

    public PlatformAdminBootstrap(
        JdbcTemplate jdbcTemplate,
        PasswordHasherPort passwordHasher,
        Clock clock,
        @Value("${app.bootstrap.platform-admin.enabled:false}") boolean enabled,
        @Value("${app.bootstrap.platform-admin.email:platform.admin@attestry.local}") String adminEmail,
        @Value("${app.bootstrap.platform-admin.password:PlatformAdm1n!2026}") String adminPassword,
        @Value("${app.bootstrap.platform-admin.phone:010-0000-0000}") String adminPhone
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
        this.enabled = enabled;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminPhone = adminPhone;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        String userId = ensureUser(normalizedEmail);

        ensureTenant();
        String membershipId = ensureMembership(userId);
        ensureAssignment(userId, membershipId);

        log.info("Platform admin bootstrap ready. email={}, password={}", normalizedEmail, adminPassword);
    }

    private String ensureUser(String normalizedEmail) {
        String existingUserId = querySingle(
            "SELECT user_id FROM user_accounts WHERE email = ?",
            normalizedEmail
        );
        if (existingUserId != null) {
            return existingUserId;
        }

        jdbcTemplate.update(
            """
            INSERT INTO user_accounts (user_id, email, password_hash, phone, status, verification_level)
            VALUES (?, ?, ?, ?, 'ACTIVE', 'PHONE_VERIFIED')
            """,
            DEFAULT_USER_ID,
            normalizedEmail,
            passwordHasher.hash(adminPassword),
            adminPhone
        );
        return DEFAULT_USER_ID;
    }

    private void ensureTenant() {
        if (exists("SELECT 1 FROM tenants WHERE tenant_id = ?", DEFAULT_TENANT_ID)) {
            return;
        }
        jdbcTemplate.update(
            """
            INSERT INTO tenants (tenant_id, name, region, type, status)
            VALUES (?, ?, ?, 'INTERNAL', 'ACTIVE')
            """,
            DEFAULT_TENANT_ID,
            "Platform Admin Tenant",
            "KR"
        );
    }

    private String ensureMembership(String userId) {
        String existingMembershipId = querySingle(
            """
            SELECT membership_id
            FROM memberships
            WHERE user_id = ? AND tenant_id = ?
            """,
            userId,
            DEFAULT_TENANT_ID
        );
        if (existingMembershipId != null) {
            jdbcTemplate.update(
                """
                UPDATE memberships
                SET group_type = 'INTERNAL',
                    status = 'ACTIVE',
                    tenant_status = 'ACTIVE'
                WHERE membership_id = ?
                """,
                existingMembershipId
            );
            return existingMembershipId;
        }

        String membershipId = DEFAULT_MEMBERSHIP_ID;
        if (exists("SELECT 1 FROM memberships WHERE membership_id = ?", membershipId)) {
            membershipId = UUID.randomUUID().toString();
        }

        jdbcTemplate.update(
            """
            INSERT INTO memberships (
                membership_id, user_id, tenant_id, group_type,
                status, tenant_status
            )
            VALUES (?, ?, ?, 'INTERNAL', 'ACTIVE', 'ACTIVE')
            """,
            membershipId,
            userId,
            DEFAULT_TENANT_ID
        );
        return membershipId;
    }

    private void ensureAssignment(String userId, String membershipId) {
        if (exists("SELECT 1 FROM membership_role_assignments WHERE membership_id = ?", membershipId)) {
            return;
        }

        String assignmentId = DEFAULT_ASSIGNMENT_ID;
        if (exists("SELECT 1 FROM membership_role_assignments WHERE assignment_id = ?", assignmentId)) {
            assignmentId = UUID.randomUUID().toString();
        }

        jdbcTemplate.update(
            """
            INSERT INTO membership_role_assignments (
                assignment_id, membership_id, role_id, assigned_by_user_id, assigned_at
            )
            VALUES (?, ?, ?, ?, ?)
            """,
            assignmentId,
            membershipId,
            PLATFORM_SUPER_ADMIN_ROLE_ID,
            userId,
            Timestamp.from(Instant.now(clock))
        );
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM (" + sql + ") t",
            Integer.class,
            args
        );
        return count != null && count > 0;
    }

    private String querySingle(String sql, Object... args) {
        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString(1) : null, args);
    }
}
