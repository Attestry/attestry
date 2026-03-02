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
import io.attestry.userauth.application.port.PasswordHasherPort;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import io.attestry.userauth.domain.user.enums.UserStatus;
import io.attestry.userauth.domain.user.enums.VerificationLevel;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.GroupJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.TenantJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.UserAccountJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.GroupJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.InvitationJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.MembershipRoleAssignmentJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OnboardingEvidenceBundleJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OnboardingEvidenceFileJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OrganizationApplicationJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.RoleAssignmentAuditJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.TenantJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.UserAccountJpaRepository;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private GroupJpaRepository groupRepository;

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
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void initMockMvc() {
        clearData();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    private void clearData() {
        jdbcTemplate.update("DELETE FROM tenant_role_template_bindings");
        roleAssignmentAuditRepository.deleteAll();
        membershipRoleAssignmentRepository.deleteAll();
        membershipRepository.deleteAll();
        invitationRepository.deleteAll();
        organizationApplicationRepository.deleteAll();
        onboardingEvidenceFileRepository.deleteAll();
        onboardingEvidenceBundleRepository.deleteAll();
        groupRepository.deleteAll();
        tenantRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void brandApplicationFlowShouldWork() throws Exception {
        String email = "brand-applicant@test.com";
        String password = "BrandPw123";

        signUp(email, password, "010-1111-2222");
        String token = login(email, password, null, null);
        String evidenceBundleId = prepareEvidenceBundle(token);

        MvcResult created = mockMvc.perform(post("/brand-applications")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "brandName", "Brand X",
                    "country", "KR",
                    "bizRegNo", "111-22-33333",
                    "evidenceBundleId", evidenceBundleId
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("BRAND"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();

        String applicationId = readJson(created).get("applicationId").asText();

        mockMvc.perform(get("/brand-applications/{id}", applicationId)
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationId").value(applicationId))
            .andExpect(jsonPath("$.orgName").value("Brand X"));

        org.assertj.core.api.Assertions.assertThat(organizationApplicationRepository.findById(applicationId)).isPresent();
    }

    @Test
    void retailOnboardingAndInvitationFlowShouldWork() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String adminGroupId = UUID.randomUUID().toString();
        String adminUserId = UUID.randomUUID().toString();

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant A", "KR", TenantStatus.ACTIVE));
        groupRepository.save(new GroupJpaEntity(adminGroupId, tenantId, GroupType.BRAND, GroupStatus.ACTIVE));

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
            adminGroupId,
            tenantId,
            GroupType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        membershipRoleAssignmentRepository.save(new io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity(
            UUID.randomUUID().toString(),
            membershipRepository.findByUserId(adminUserId).getFirst().getMembershipId(),
            "role-platform-super-admin",
            null,
            Instant.now()
        ));

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
            adminGroupId,
            tenantId,
            GroupType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        membershipRoleAssignmentRepository.save(new io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity(
            UUID.randomUUID().toString(),
            membershipAdminMembership.getMembershipId(),
            "role-tenant-owner",
            null,
            Instant.now()
        ));
        bindTenantOwnerTemplate(tenantId, membershipAdminUserId);

        String applicantEmail = "retail-applicant@test.com";
        String applicantPassword = "ApplicantPw123";
        signUp(applicantEmail, applicantPassword, "010-2222-3333");

        String applicantToken = login(applicantEmail, applicantPassword, null, null);
        String adminToken = login(adminEmail, "AdminPw123", tenantId, adminGroupId);
        String membershipAdminToken = login(membershipAdminEmail, "MemberAdminPw123", tenantId, adminGroupId);
        String evidenceBundleId = prepareEvidenceBundle(applicantToken);

        MvcResult retailCreated = mockMvc.perform(post("/retail-applications")
                .header("Authorization", bearer(applicantToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "retailName", "Retail One",
                    "country", "KR",
                    "bizRegNo", "999-88-77777",
                    "evidenceBundleId", evidenceBundleId
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("RETAIL"))
            .andReturn();

        String retailAppId = readJson(retailCreated).get("applicationId").asText();

        MvcResult approved = mockMvc.perform(post("/admin/retail-applications/{id}/approve", retailAppId)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andReturn();

        String retailTenantId = readJson(approved).get("tenantId").asText();
        String retailGroupId = readJson(approved).get("groupId").asText();
        String applicantUserId = userAccountRepository.findByEmail(applicantEmail).orElseThrow().getUserId();
        MembershipJpaEntity retailAdminMembership = membershipRepository
            .findByUserIdAndTenantIdAndGroupId(applicantUserId, retailTenantId, retailGroupId)
            .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(
            membershipRoleAssignmentRepository.findByMembershipId(retailAdminMembership.getMembershipId())
                .orElseThrow()
                .getRoleId()
        ).isEqualTo("role-tenant-owner");
        String retailAdminToken = login(applicantEmail, applicantPassword, retailTenantId, retailGroupId);

        String inviteeEmail = "staff@test.com";
        String inviteePassword = "StaffPw123";
        signUp(inviteeEmail, inviteePassword, "010-3333-4444");

        MvcResult invitationCreated = mockMvc.perform(post("/tenants/{tenantId}/admin/invitations", retailTenantId)
                .header("Authorization", bearer(retailAdminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "email", inviteeEmail,
                    "groupId", retailGroupId,
                    "role", "STAFF"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();

        String invitationId = readJson(invitationCreated).get("invitationId").asText();
        String inviteeToken = login(inviteeEmail, inviteePassword, null, null);

        mockMvc.perform(post("/invitations/{id}/accept", invitationId)
                .header("Authorization", bearer(inviteeToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.groupId").value(retailGroupId))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/tenants/{tenantId}/admin/memberships", retailTenantId)
                .header("Authorization", bearer(retailAdminToken)))
            .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(invitationRepository.findById(invitationId)).isPresent();
        org.assertj.core.api.Assertions.assertThat(membershipRepository.findByTenantId(retailTenantId)).isNotEmpty();
    }

    @Test
    void membershipUpdateShouldWork() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String adminGroupId = UUID.randomUUID().toString();
        String adminUserId = UUID.randomUUID().toString();
        String targetUserId = UUID.randomUUID().toString();

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant B", "KR", TenantStatus.ACTIVE));
        groupRepository.save(new GroupJpaEntity(adminGroupId, tenantId, GroupType.BRAND, GroupStatus.ACTIVE));

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
            adminGroupId,
            tenantId,
            GroupType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        membershipRoleAssignmentRepository.save(new io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity(
            UUID.randomUUID().toString(),
            membershipRepository.findByUserId(adminUserId).getFirst().getMembershipId(),
            "role-tenant-owner",
            null,
            Instant.now()
        ));
        bindTenantOwnerTemplate(tenantId, adminUserId);

        MembershipJpaEntity targetMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            targetUserId,
            adminGroupId,
            tenantId,
            GroupType.BRAND,
            MembershipRole.STAFF,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));

        String adminToken = login("membership-admin@test.com", "AdminPw123", tenantId, adminGroupId);

        mockMvc.perform(post("/tenants/{tenantId}/admin/memberships/{id}/roles/{roleCode}", tenantId, targetMembership.getMembershipId(), "TENANT_OPERATOR")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.membershipId").value(targetMembership.getMembershipId()))
            .andExpect(jsonPath("$.roleCodes[?(@ == 'TENANT_OPERATOR')]").exists());

        mockMvc.perform(patch("/tenants/{tenantId}/admin/memberships/{id}/role", tenantId, targetMembership.getMembershipId())
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("role", "OPERATOR"))))
            .andExpect(status().isNotFound());

        mockMvc.perform(patch("/tenants/{tenantId}/admin/memberships/{id}/status", tenantId, targetMembership.getMembershipId())
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "status", "SUSPENDED"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void groupSuspendAndUnsuspendShouldSyncMembershipGroupStatus() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String adminGroupId = UUID.randomUUID().toString();
        String adminUserId = UUID.randomUUID().toString();
        String memberUserId = UUID.randomUUID().toString();

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant C", "KR", TenantStatus.ACTIVE));
        groupRepository.save(new GroupJpaEntity(adminGroupId, tenantId, GroupType.RETAIL, GroupStatus.ACTIVE));

        userAccountRepository.save(new UserAccountJpaEntity(
            adminUserId,
            "group-admin@test.com",
            passwordHasher.hash("AdminPw123"),
            "010-7777-1111",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));

        userAccountRepository.save(new UserAccountJpaEntity(
            memberUserId,
            "group-member@test.com",
            passwordHasher.hash("MemberPw123"),
            "010-7777-2222",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));

        membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            adminUserId,
            adminGroupId,
            tenantId,
            GroupType.RETAIL,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        membershipRoleAssignmentRepository.save(new io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity(
            UUID.randomUUID().toString(),
            membershipRepository.findByUserId(adminUserId).getFirst().getMembershipId(),
            "role-tenant-owner",
            null,
            Instant.now()
        ));
        bindTenantOwnerTemplate(tenantId, adminUserId);

        MembershipJpaEntity memberMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            memberUserId,
            adminGroupId,
            tenantId,
            GroupType.RETAIL,
            MembershipRole.STAFF,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));

        String adminToken = login("group-admin@test.com", "AdminPw123", tenantId, adminGroupId);

        mockMvc.perform(post("/tenants/{tenantId}/admin/groups/{id}/suspend", tenantId, adminGroupId)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUSPENDED"));

        org.assertj.core.api.Assertions.assertThat(groupRepository.findById(adminGroupId))
            .get()
            .extracting(GroupJpaEntity::getStatus)
            .isEqualTo(GroupStatus.SUSPENDED);
        org.assertj.core.api.Assertions.assertThat(membershipRepository.findById(memberMembership.getMembershipId()))
            .get()
            .extracting(MembershipJpaEntity::getGroupStatus)
            .isEqualTo(GroupStatus.SUSPENDED);

        mockMvc.perform(post("/tenants/{tenantId}/admin/groups/{id}/unsuspend", tenantId, adminGroupId)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        org.assertj.core.api.Assertions.assertThat(groupRepository.findById(adminGroupId))
            .get()
            .extracting(GroupJpaEntity::getStatus)
            .isEqualTo(GroupStatus.ACTIVE);
        org.assertj.core.api.Assertions.assertThat(membershipRepository.findById(memberMembership.getMembershipId()))
            .get()
            .extracting(MembershipJpaEntity::getGroupStatus)
            .isEqualTo(GroupStatus.ACTIVE);
    }

    @Test
    void roleAssignmentApiShouldAssignListAndRevoke() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String groupId = UUID.randomUUID().toString();
        String adminUserId = UUID.randomUUID().toString();
        String targetUserId = UUID.randomUUID().toString();

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant D", "KR", TenantStatus.ACTIVE));
        groupRepository.save(new GroupJpaEntity(groupId, tenantId, GroupType.BRAND, GroupStatus.ACTIVE));

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
            groupId,
            tenantId,
            GroupType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        membershipRoleAssignmentRepository.save(new io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity(
            UUID.randomUUID().toString(),
            adminMembership.getMembershipId(),
            "role-tenant-owner",
            null,
            Instant.now()
        ));
        bindTenantOwnerTemplate(tenantId, adminUserId);

        MembershipJpaEntity targetMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            targetUserId,
            groupId,
            tenantId,
            GroupType.BRAND,
            MembershipRole.STAFF,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        membershipRoleAssignmentRepository.save(new io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity(
            UUID.randomUUID().toString(),
            targetMembership.getMembershipId(),
            "role-tenant-staff",
            null,
            Instant.now()
        ));

        String adminToken = login("assign-admin@test.com", "AdminPw123", tenantId, groupId);

        mockMvc.perform(post("/tenants/{tenantId}/admin/memberships/{id}/roles/{roleCode}", tenantId, targetMembership.getMembershipId(), "TENANT_OPERATOR")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.membershipId").value(targetMembership.getMembershipId()))
            .andExpect(jsonPath("$.roleCodes").isArray())
            .andExpect(jsonPath("$.roleCodes[?(@ == 'TENANT_OPERATOR')]").exists());

        mockMvc.perform(get("/tenants/{tenantId}/admin/memberships/{id}/roles", tenantId, targetMembership.getMembershipId())
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleCodes[?(@ == 'TENANT_OPERATOR')]").exists());

        mockMvc.perform(delete("/tenants/{tenantId}/admin/memberships/{id}/roles/{roleCode}", tenantId, targetMembership.getMembershipId(), "TENANT_OPERATOR")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleCodes[?(@ == 'TENANT_OPERATOR')]").doesNotExist());
    }

    @Test
    void roleAssignmentApiShouldAllowTenantOwnerToAssignTenantOwner() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        String groupId = UUID.randomUUID().toString();
        String adminUserId = UUID.randomUUID().toString();
        String targetUserId = UUID.randomUUID().toString();

        tenantRepository.save(new TenantJpaEntity(tenantId, "Tenant E", "KR", TenantStatus.ACTIVE));
        groupRepository.save(new GroupJpaEntity(groupId, tenantId, GroupType.BRAND, GroupStatus.ACTIVE));

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
            groupId,
            tenantId,
            GroupType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));
        membershipRoleAssignmentRepository.save(new io.attestry.userauth.infrastructure.persistence.jpa.entity.MembershipRoleAssignmentJpaEntity(
            UUID.randomUUID().toString(),
            adminMembership.getMembershipId(),
            "role-tenant-owner",
            null,
            Instant.now()
        ));
        bindTenantOwnerTemplate(tenantId, adminUserId);

        MembershipJpaEntity targetMembership = membershipRepository.save(new MembershipJpaEntity(
            UUID.randomUUID().toString(),
            targetUserId,
            groupId,
            tenantId,
            GroupType.BRAND,
            MembershipRole.STAFF,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        ));

        String adminToken = login("sensitive-admin@test.com", "AdminPw123", tenantId, groupId);

        mockMvc.perform(post("/tenants/{tenantId}/admin/memberships/{id}/roles/{roleCode}", tenantId, targetMembership.getMembershipId(), "TENANT_OWNER")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.membershipId").value(targetMembership.getMembershipId()))
            .andExpect(jsonPath("$.roleCodes").isArray())
            .andExpect(jsonPath("$.roleCodes").value(org.hamcrest.Matchers.hasItem("TENANT_OWNER")));
    }

    private void signUp(String email, String password, String phone) throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "email", email,
                    "password", password,
                    "phone", phone
                ))))
            .andExpect(status().isCreated());
    }

    private String login(String email, String password, String tenantId, String groupId) throws Exception {
        ObjectNode payloadNode = objectMapper.createObjectNode();
        payloadNode.put("email", email);
        payloadNode.put("password", password);
        if (tenantId == null) {
            payloadNode.putNull("tenantId");
        } else {
            payloadNode.put("tenantId", tenantId);
        }
        if (groupId == null) {
            payloadNode.putNull("groupId");
        } else {
            payloadNode.put("groupId", groupId);
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
            .andExpect(jsonPath("$.status").value("READY"));

        return evidenceBundleId;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void bindTenantOwnerTemplate(String tenantId, String actorUserId) {
        jdbcTemplate.update(
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
    }
}
