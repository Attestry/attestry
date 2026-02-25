package io.attestry;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OrganizationApplicationJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.TenantJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.UserAccountJpaRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
    private OrganizationApplicationJpaRepository organizationApplicationRepository;

    @Autowired
    private InvitationJpaRepository invitationRepository;

    @BeforeEach
    void initMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void brandApplicationFlowShouldWork() throws Exception {
        String email = "brand-applicant@test.com";
        String password = "pw1234";

        signUp(email, password, "010-1111-2222");
        String token = login(email, password, null, null);

        MvcResult created = mockMvc.perform(post("/brand-applications")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "brandName", "Brand X",
                    "country", "KR",
                    "bizRegNo", "111-22-33333",
                    "evidenceGroupId", UUID.randomUUID().toString()
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
            passwordHasher.hash("adminpw"),
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

        String applicantEmail = "retail-applicant@test.com";
        String applicantPassword = "applicantpw";
        signUp(applicantEmail, applicantPassword, "010-2222-3333");

        String applicantToken = login(applicantEmail, applicantPassword, null, null);
        String adminToken = login(adminEmail, "adminpw", tenantId, adminGroupId);

        MvcResult retailCreated = mockMvc.perform(post("/tenants/{tenantId}/retail-applications", tenantId)
                .header("Authorization", bearer(applicantToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "retailName", "Retail One",
                    "country", "KR",
                    "bizRegNo", "999-88-77777",
                    "evidenceGroupId", UUID.randomUUID().toString()
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("RETAIL"))
            .andReturn();

        String retailAppId = readJson(retailCreated).get("applicationId").asText();

        MvcResult approved = mockMvc.perform(post("/tenants/{tenantId}/admin/retail-applications/{id}/approve", tenantId, retailAppId)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantId").value(tenantId))
            .andReturn();

        String retailGroupId = readJson(approved).get("groupId").asText();

        String inviteeEmail = "staff@test.com";
        String inviteePassword = "staffpw";
        signUp(inviteeEmail, inviteePassword, "010-3333-4444");

        MvcResult invitationCreated = mockMvc.perform(post("/tenants/{tenantId}/admin/invitations", tenantId)
                .header("Authorization", bearer(adminToken))
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

        mockMvc.perform(get("/tenants/{tenantId}/admin/memberships", tenantId)
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(invitationRepository.findById(invitationId)).isPresent();
        org.assertj.core.api.Assertions.assertThat(membershipRepository.findByTenantId(tenantId)).isNotEmpty();
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
            passwordHasher.hash("adminpw"),
            "010-5555-1111",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        ));

        userAccountRepository.save(new UserAccountJpaEntity(
            targetUserId,
            "membership-target@test.com",
            passwordHasher.hash("targetpw"),
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

        String adminToken = login("membership-admin@test.com", "adminpw", tenantId, adminGroupId);

        mockMvc.perform(patch("/tenants/{tenantId}/admin/memberships/{id}", tenantId, targetMembership.getMembershipId())
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "role", "OPERATOR",
                    "status", "SUSPENDED"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("OPERATOR"))
            .andExpect(jsonPath("$.status").value("SUSPENDED"));
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

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
