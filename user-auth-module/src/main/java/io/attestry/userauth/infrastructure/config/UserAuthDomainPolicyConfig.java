package io.attestry.userauth.infrastructure.config;

import io.attestry.userauth.domain.membership.policy.DefaultRoleAssignmentPolicy;
import io.attestry.userauth.domain.authorization.policy.DefaultTemplateApplicationPolicy;
import io.attestry.userauth.domain.membership.policy.RoleAssignmentDecisionPolicy;
import io.attestry.userauth.domain.membership.service.RoleAssignmentDomainService;
import io.attestry.userauth.domain.membership.policy.RoleAssignmentPolicy;
import io.attestry.userauth.domain.authorization.service.TemplateApplicationDomainService;
import io.attestry.userauth.domain.authorization.policy.TemplateApplicationPolicy;
import io.attestry.userauth.domain.authorization.policy.TenantRoleTemplateBindingPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAuthDomainPolicyConfig {

    @Bean
    public RoleAssignmentPolicy roleAssignmentPolicy() {
        return new DefaultRoleAssignmentPolicy();
    }

    @Bean
    public RoleAssignmentDecisionPolicy roleAssignmentDecisionPolicy(RoleAssignmentPolicy roleAssignmentPolicy) {
        return new RoleAssignmentDecisionPolicy(roleAssignmentPolicy);
    }

    @Bean
    public RoleAssignmentDomainService roleAssignmentDomainService(RoleAssignmentDecisionPolicy roleAssignmentDecisionPolicy) {
        return new RoleAssignmentDomainService(roleAssignmentDecisionPolicy);
    }

    @Bean
    public TemplateApplicationPolicy templateApplicationPolicy() {
        return new DefaultTemplateApplicationPolicy();
    }

    @Bean
    public TemplateApplicationDomainService templateApplicationDomainService(TemplateApplicationPolicy templateApplicationPolicy) {
        return new TemplateApplicationDomainService(templateApplicationPolicy);
    }

    @Bean
    public TenantRoleTemplateBindingPolicy tenantRoleTemplateBindingPolicy() {
        return new TenantRoleTemplateBindingPolicy();
    }
}
