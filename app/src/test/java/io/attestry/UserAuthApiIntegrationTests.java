package io.attestry;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.attestry.userauth.application.port.auth.PasswordHasherPort;
import io.attestry.userauth.domain.tenant.model.TenantType;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import io.attestry.userauth.domain.auth.model.UserStatus;
import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.TenantJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.UserAccountJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.InvitationJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipRoleAssignmentJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OnboardingEvidenceBundleJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OnboardingEvidenceFileJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OrganizationApplicationJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.RoleAssignmentAuditJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.TenantJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.UserAccountJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.membership.MembershipEffectivePermissionProjectionRefresher;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class UserAuthApiIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PasswordHasherPort passwordHasher;

    @Autowired
    private UserAccountJpaRepository userAccountRepository;

    @Autowired
    private TenantJpaRepository tenantRepository;

    @Autowired
    private MembershipJpaRepository membershipRepository;

    @Autowired
    private MembershipRoleAssignmentJpaRepository membershipRoleAssignmentRepository;

    @Autowired
    private OrganizationApplicationJpaRepository organizationApplicationRepository;

    @Autowired
    private OnboardingEvidenceBundleJpaRepository onboardingEvidenceBundleRepository;

    @Autowired
    private OnboardingEvidenceFileJpaRepository onboardingEvidenceFileRepository;

    @Autowired
    private InvitationJpaRepository invitationRepository;

    @Autowired
    private RoleAssignmentAuditJpaRepository roleAssignmentAuditRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private MembershipEffectivePermissionProjectionRefresher permissionProjectionRefresher;

    @BeforeEach
    void initMockMvc() {
        clearData();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    private void clearData() {
        jdbcTemplate.getJdbcOperations().update("DELETE FROM membership_effective_permissions");
        jdbcTemplate.getJdbcOperations().update("DELETE FROM tenant_role_template_bindings");
        roleAssignmentAuditRepository.deleteAll();
        membershipRoleAssignmentRepository.deleteAll();
        membershipRepository.deleteAll();
        invitationRepository.deleteAll();
        organizationApplicationRepository.deleteAll();
        onboardingEvidenceFileRepository.deleteAll();
        onboardingEvidenceBundleRepository.deleteAll();
        tenantRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void brandApplicationFlowShouldWork() throws Exception {
        String email = "brand-applicant@test.com";
        String password = "BrandPw123";

        signUp(email, password, "010-1111-2222");
        String token = login(email, password, null);
        String evidenceBundleId = prepareEvidenceBundle(token);

        MvcResult created = mockMvc.perform(post("/onboarding/applications")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "type", "BRAND",
                    "orgName", "Brand X",
                    "country", "KR",
                    "address", "서울 강남구 테헤란로 152",
                    "bizRegNo", "111-22-33333",
                    "evidenceBundleId", evidenceBundleId
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.type").value("BRAND"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn();

        String applicationId = readJson(created).get("applicationId").asText();

        mockMvc.perform(get("/onboarding/applications/me/{id}", applicationId)
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.applicationId").value(applicationId))
            .andExpect(jsonPath("$.data.orgName").value("Brand X"));

        org.assertj.core.api.Assertions.assertThat(organizationApplicationRepository.findById(applicationId)).isPresent();
    }

    @Test
    void retailOnboardingAndInvitationFlowShouldWork() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String adminUserId = UUID.randomUUID().toString();

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant A", "KR", "서울 강남구 테헤란로 201", TenantType.BRAND, TenantStatus.ACTIVE));

        String adminEmail = "tenant-admin@test.com";
        userAccountRepository.save(new UserAccountJpaEntity(
            adminUserId,
            adminEmail,
            passwordHasher.hash("AdminPw123"),
            "010-9999-9999",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));

        membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            adminUserId,
            tenantId,
            TenantType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        assignRoleToMembership(
            membershipRepository.findByUserId(adminUserId).getFirst().getMembershipId(),
            "role-platform-super-admin"
        );

        String membershipAdminUserId = UUID.randomUUID().toString();
        String membershipAdminEmail = "tenant-membership-admin@test.com";
        userAccountRepository.save(new UserAccountJpaEntity(
            membershipAdminUserId,
            membershipAdminEmail,
            passwordHasher.hash("MemberAdminPw123"),
            "010-1234-5678",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));
        MembershipJpaEntity membershipAdminMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            membershipAdminUserId,
            tenantId,
            TenantType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        assignRoleToMembership(membershipAdminMembership.getMembershipId(), "role-tenant-owner");
        bindTenantOwnerTemplate(tenantId, membershipAdminUserId);

        String applicantEmail = "retail-applicant@test.com";
        String applicantPassword = "ApplicantPw123";
        signUp(applicantEmail, applicantPassword, "010-2222-3333");

        String applicantToken = login(applicantEmail, applicantPassword, null);
        String adminToken = login(adminEmail, "AdminPw123", tenantId);
        String membershipAdminToken = login(membershipAdminEmail, "MemberAdminPw123", tenantId);
        String evidenceBundleId = prepareEvidenceBundle(applicantToken);

        MvcResult retailCreated = mockMvc.perform(post("/onboarding/applications")
                .header("Authorization", bearer(applicantToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "type", "RETAIL",
                    "orgName", "Retail One",
                    "country", "KR",
                    "address", "서울 성동구 아차산로13길 11",
                    "bizRegNo", "999-88-77777",
                    "evidenceBundleId", evidenceBundleId
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.type").value("RETAIL"))
            .andReturn();

        String retailAppId = readJson(retailCreated).get("applicationId").asText();

        MvcResult approved = mockMvc.perform(post("/onboarding/applications/{id}/approve", retailAppId)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andReturn();

        String retailTenantId = readJson(approved).get("tenantId").asText();
        String applicantUserId = userAccountRepository.findByEmail(applicantEmail).orElseThrow().getUserId();
        MembershipJpaEntity retailAdminMembership = membershipRepository
            .findByUserIdAndTenantId(applicantUserId, retailTenantId)
            .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(
            membershipRoleAssignmentRepository.findByMembershipId(retailAdminMembership.getMembershipId())
                .orElseThrow()
                .getRoleId()
        ).isEqualTo("role-tenant-owner");
        String retailAdminToken = login(applicantEmail, applicantPassword, retailTenantId);

        String inviteeEmail = "staff@test.com";
        String inviteePassword = "StaffPw123";
        signUp(inviteeEmail, inviteePassword, "010-3333-4444");

        MvcResult invitationCreated = mockMvc.perform(post("/invitations")
                .header("Authorization", bearer(retailAdminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "email", inviteeEmail,
                    "role", "STAFF"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn();

        String invitationId = readJson(invitationCreated).get("invitationId").asText();
        String inviteeToken = login(inviteeEmail, inviteePassword, null);

        mockMvc.perform(post("/invitations/{invitationId}/accept", invitationId)
                .header("Authorization", bearer(inviteeToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/memberships")
                .header("Authorization", bearer(retailAdminToken)))
            .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(invitationRepository.findById(invitationId)).isPresent();
        org.assertj.core.api.Assertions.assertThat(membershipRepository.findByTenantId(retailTenantId)).isNotEmpty();
    }

    @Test
    void operatorInvitationAcceptShouldGrantTenantOperatorRole() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String adminUserId = UUID.randomUUID().toString();
        String adminEmail = "operator-invite-admin@test.com";
        String adminPassword = "AdminPw123";

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant C", "KR", "서울 강남구 봉은사로 301", TenantType.BRAND, TenantStatus.ACTIVE));

        userAccountRepository.save(new UserAccountJpaEntity(
            adminUserId,
            adminEmail,
            passwordHasher.hash(adminPassword),
            "010-7777-1111",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));

        MembershipJpaEntity adminMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            adminUserId,
            tenantId,
            TenantType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        assignRoleToMembership(adminMembership.getMembershipId(), "role-tenant-owner");
        bindTenantOwnerTemplate(tenantId, adminUserId);

        String inviteeEmail = "operator-invitee@test.com";
        String inviteePassword = "OperatorPw123";
        signUp(inviteeEmail, inviteePassword, "010-7777-2222");

        String adminToken = login(adminEmail, adminPassword, tenantId);
        MvcResult invitationCreated = mockMvc.perform(post("/invitations")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "email", inviteeEmail,
                    "role", "OPERATOR"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.role").value("OPERATOR"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn();

        String invitationId = readJson(invitationCreated).get("invitationId").asText();
        String inviteeToken = login(inviteeEmail, inviteePassword, null);

        mockMvc.perform(post("/invitations/{invitationId}/accept", invitationId)
                .header("Authorization", bearer(inviteeToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.roleCodes").isArray())
            .andExpect(jsonPath("$.data.roleCodes").value(org.hamcrest.Matchers.hasItem("TENANT_OPERATOR")));

        String inviteeUserId = userAccountRepository.findByEmail(inviteeEmail).orElseThrow().getUserId();
        MembershipJpaEntity inviteeMembership = membershipRepository
            .findByUserIdAndTenantId(inviteeUserId, tenantId)
            .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(
            membershipRoleAssignmentRepository.findRoleCodesByMembershipId(inviteeMembership.getMembershipId())
        ).contains("TENANT_OPERATOR");
    }

    @Test
    void signupShouldReturnValidationMessageWhenPasswordRuleIsViolated() throws Exception {
        String email = "invalid-password@test.com";

        mockMvc.perform(post("/auth/signup/email-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/signup/email-verifications/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "email", email,
                    "code", "12345678"
                ))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "email", email,
                    "password", "qwer1234",
                    "phone", "010-1234-5678"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.error.message").value("비밀번호는 8자 이상이며 영문 대문자를 1자 이상 포함해야 합니다"));
    }

    @Test
    void signupEmailVerificationRequestShouldReturnLocalizedMessageWhenEmailAlreadyExists() throws Exception {
        String email = "duplicate-email@test.com";
        signUp(email, "DuplicatePw123", "010-1234-5678");

        mockMvc.perform(post("/auth/signup/email-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"))
            .andExpect(jsonPath("$.error.message").value("이미 등록된 이메일입니다"));
    }

    @Test
    void signupEmailVerificationRequestShouldRejectInvalidTopLevelDomain() throws Exception {
        mockMvc.perform(post("/auth/signup/email-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "qwer@asdf.123"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.error.message").value("올바른 이메일 형식을 입력해주세요."));
    }

    @Test
    void membershipUpdateShouldWork() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String adminUserId = UUID.randomUUID().toString();
        String targetUserId = UUID.randomUUID().toString();

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant B", "KR", "서울 서초구 강남대로 221", TenantType.BRAND, TenantStatus.ACTIVE));

        userAccountRepository.save(new UserAccountJpaEntity(
            adminUserId,
            "membership-admin@test.com",
            passwordHasher.hash("AdminPw123"),
            "010-5555-1111",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));

        userAccountRepository.save(new UserAccountJpaEntity(
            targetUserId,
            "membership-target@test.com",
            passwordHasher.hash("TargetPw123"),
            "010-5555-2222",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));

        membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            adminUserId,
            tenantId,
            TenantType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        assignRoleToMembership(
            membershipRepository.findByUserId(adminUserId).getFirst().getMembershipId(),
            "role-tenant-owner"
        );
        bindTenantOwnerTemplate(tenantId, adminUserId);

        MembershipJpaEntity targetMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            targetUserId,
            tenantId,
            TenantType.BRAND,
            MembershipRole.STAFF,
            MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));

        String adminToken = login("membership-admin@test.com", "AdminPw123", tenantId);

        mockMvc.perform(post("/memberships/{id}/roles/{roleCode}",targetMembership.getMembershipId(), "TENANT_OPERATOR")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.membershipId").value(targetMembership.getMembershipId()))
            .andExpect(jsonPath("$.data.roleCodes[?(@ == 'TENANT_OPERATOR')]").exists());

        mockMvc.perform(patch("/memberships/{id}/role",targetMembership.getMembershipId())
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("role", "OPERATOR"))))
            .andExpect(status().isNotFound());

        mockMvc.perform(patch("/memberships/{id}/status",targetMembership.getMembershipId())
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "status", "SUSPENDED"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    void roleAssignmentApiShouldAssignListAndRevoke() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String adminUserId = UUID.randomUUID().toString();
        String targetUserId = UUID.randomUUID().toString();

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant D", "KR", "서울 송파구 올림픽로 240", TenantType.BRAND, TenantStatus.ACTIVE));

        userAccountRepository.save(new UserAccountJpaEntity(
            adminUserId,
            "assign-admin@test.com",
            passwordHasher.hash("AdminPw123"),
            "010-8888-1111",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));
        userAccountRepository.save(new UserAccountJpaEntity(
            targetUserId,
            "assign-target@test.com",
            passwordHasher.hash("TargetPw123"),
            "010-8888-2222",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));

        MembershipJpaEntity adminMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            adminUserId,
            tenantId,
            TenantType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        assignRoleToMembership(adminMembership.getMembershipId(), "role-tenant-owner");
        bindTenantOwnerTemplate(tenantId, adminUserId);

        MembershipJpaEntity targetMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            targetUserId,
            tenantId,
            TenantType.BRAND,
            MembershipRole.STAFF,
            MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        assignRoleToMembership(targetMembership.getMembershipId(), "role-tenant-staff");

        String adminToken = login("assign-admin@test.com", "AdminPw123", tenantId);

        mockMvc.perform(post("/memberships/{id}/roles/{roleCode}",targetMembership.getMembershipId(), "TENANT_OPERATOR")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.membershipId").value(targetMembership.getMembershipId()))
            .andExpect(jsonPath("$.data.roleCodes").isArray())
            .andExpect(jsonPath("$.data.roleCodes[?(@ == 'TENANT_OPERATOR')]").exists());

        mockMvc.perform(get("/memberships/{id}/roles",targetMembership.getMembershipId())
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleCodes[?(@ == 'TENANT_OPERATOR')]").exists());

        mockMvc.perform(delete("/memberships/{id}/roles/{roleCode}",targetMembership.getMembershipId(), "TENANT_OPERATOR")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roleCodes[?(@ == 'TENANT_OPERATOR')]").doesNotExist());
    }

    @Test
    void roleAssignmentApiShouldAllowTenantOwnerToAssignTenantOwner() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String adminUserId = UUID.randomUUID().toString();
        String targetUserId = UUID.randomUUID().toString();

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant E", "KR", "서울 마포구 양화로 98", TenantType.BRAND, TenantStatus.ACTIVE));

        userAccountRepository.save(new UserAccountJpaEntity(
            adminUserId,
            "sensitive-admin@test.com",
            passwordHasher.hash("AdminPw123"),
            "010-9999-1111",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));
        userAccountRepository.save(new UserAccountJpaEntity(
            targetUserId,
            "sensitive-target@test.com",
            passwordHasher.hash("TargetPw123"),
            "010-9999-2222",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));

        MembershipJpaEntity adminMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            adminUserId,
            tenantId,
            TenantType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        assignRoleToMembership(adminMembership.getMembershipId(), "role-tenant-owner");
        bindTenantOwnerTemplate(tenantId, adminUserId);

        MembershipJpaEntity targetMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            targetUserId,
            tenantId,
            TenantType.BRAND,
            MembershipRole.STAFF,
            MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));

        String adminToken = login("sensitive-admin@test.com", "AdminPw123", tenantId);

        mockMvc.perform(post("/memberships/{id}/roles/{roleCode}",targetMembership.getMembershipId(), "TENANT_OWNER")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.membershipId").value(targetMembership.getMembershipId()))
            .andExpect(jsonPath("$.data.roleCodes").isArray())
            .andExpect(jsonPath("$.data.roleCodes").value(org.hamcrest.Matchers.hasItem("TENANT_OWNER")));
    }

    @Test
    void membershipStatusApiShouldRejectSuspendingLastActiveOwner() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String ownerUserId = UUID.randomUUID().toString();

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant F", "KR", "서울 영등포구 여의대로 24", TenantType.BRAND, TenantStatus.ACTIVE));

        userAccountRepository.save(new UserAccountJpaEntity(
            ownerUserId,
            "last-owner@test.com",
            passwordHasher.hash("OwnerPw123"),
            "010-9999-3333",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));

        MembershipJpaEntity ownerMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            ownerUserId,
            tenantId,
            TenantType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        assignRoleToMembership(ownerMembership.getMembershipId(), "role-tenant-owner");
        bindTenantOwnerTemplate(tenantId, ownerUserId);

        String ownerToken = login("last-owner@test.com", "OwnerPw123", tenantId);

        mockMvc.perform(patch("/memberships/{id}/status", ownerMembership.getMembershipId())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", "SUSPENDED"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("LAST_ACTIVE_OWNER_REQUIRED"))
            .andExpect(jsonPath("$.error.message").value("시스템에 최소 한 명의 관리자가 필요합니다."));
    }

    @Test
    void roleAssignmentApiShouldRejectRevokingTenantOwnerFromLastActiveOwner() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String ownerUserId = UUID.randomUUID().toString();

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant G", "KR", "서울 중구 을지로 120", TenantType.BRAND, TenantStatus.ACTIVE));

        userAccountRepository.save(new UserAccountJpaEntity(
            ownerUserId,
            "last-owner-role@test.com",
            passwordHasher.hash("OwnerPw123"),
            "010-9999-4444",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));

        MembershipJpaEntity ownerMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            ownerUserId,
            tenantId,
            TenantType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        assignRoleToMembership(ownerMembership.getMembershipId(), "role-tenant-owner");
        bindTenantOwnerTemplate(tenantId, ownerUserId);

        String ownerToken = login("last-owner-role@test.com", "OwnerPw123", tenantId);

        mockMvc.perform(delete("/memberships/{id}/roles/{roleCode}", ownerMembership.getMembershipId(), "TENANT_OWNER")
                .header("Authorization", bearer(ownerToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("LAST_ACTIVE_OWNER_REQUIRED"))
            .andExpect(jsonPath("$.error.message").value("시스템에 최소 한 명의 관리자가 필요합니다."));
    }

    private void signUp(String email, String password, String phone) throws Exception {
        mockMvc.perform(post("/auth/signup/email-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/signup/email-verifications/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "email", email,
                    "code", "12345678"
                ))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "email", email,
                    "password", password,
                    "phone", phone
                ))))
            .andExpect(status().isCreated());
    }

    private String login(String email, String password, String tenantId) throws Exception {
        ObjectNode payloadNode = objectMapper.createObjectNode();
        payloadNode.put("email", email);
        payloadNode.put("password", password);
        if (tenantId == null) {
            payloadNode.putNull("tenantId");
        } else {
            payloadNode.put("tenantId", tenantId);
        }

        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadNode.toString()))
            .andExpect(status().isOk())
            .andReturn();

        return readJson(result).get("accessToken").asText();
    }

    private String prepareEvidenceBundle(String accessToken) throws Exception {
        MvcResult presign = mockMvc.perform(post("/onboarding/evidences/presign")
                .header("Authorization", bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "fileName", "biz-reg.pdf",
                    "contentType", "application/pdf"
                ))))
            .andExpect(status().isCreated())
            .andReturn();

        String evidenceBundleId = readJson(presign).get("evidenceBundleId").asText();
        String evidenceFileId = readJson(presign).get("evidenceFileId").asText();

        mockMvc.perform(post("/onboarding/evidences/complete")
                .header("Authorization", bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "evidenceBundleId", evidenceBundleId,
                    "evidenceFileId", evidenceFileId,
                    "sizeBytes", 1024
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("READY"));

        return evidenceBundleId;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.get("data");
        return data != null && !data.isNull() ? data : root;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void bindTenantOwnerTemplate(String tenantId, String actorUserId) {
        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO tenant_role_template_bindings (
                    binding_id,
                    tenant_id,
                    role_code,
                    template_id,
                    enabled,
                    created_by_user_id,
                    created_at
                )
                SELECT ?, ?, 'TENANT_OWNER', pt.template_id, TRUE, ?, CURRENT_TIMESTAMP
                FROM permission_templates pt
                WHERE pt.code = 'TEMPLATE_TENANT_OWNER_CORE'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM tenant_role_template_bindings trtb
                      WHERE trtb.tenant_id = ?
                        AND trtb.role_code = 'TENANT_OWNER'
                        AND trtb.template_id = pt.template_id
                  )
                """,
            UUID.randomUUID().toString(),
            tenantId,
            actorUserId,
            tenantId
        );
        permissionProjectionRefresher.refreshTenant(tenantId);
    }

    private void assignRoleToMembership(String membershipId, String roleId) {
        membershipRoleAssignmentRepository.save(new io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity(
            UUID.randomUUID().toString(),
            membershipId,
            roleId,
            null,
            Instant.now()
        ));
        permissionProjectionRefresher.refreshMembership(membershipId);
    }
}
