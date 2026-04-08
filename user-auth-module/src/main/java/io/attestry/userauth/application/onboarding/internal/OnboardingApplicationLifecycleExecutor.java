package io.attestry.userauth.application.onboarding.internal;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.onboarding.command.CreateApplicationCommand;
import io.attestry.userauth.application.onboarding.result.ApplicationResult;
import io.attestry.userauth.application.onboarding.result.ApproveApplicationResult;
import io.attestry.userauth.application.onboarding.internal.OnboardingProvisioningService.ProvisioningResult;
import io.attestry.userauth.application.port.onboarding.OrganizationApplicationRepositoryPort;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.onboarding.policy.OrganizationUniquenessPolicy;
import io.attestry.userauth.domain.tenant.model.TenantType;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingApplicationLifecycleExecutor {

    private final OrganizationApplicationRepositoryPort applicationRepository;
    private final OnboardingApplicationCommandPolicy commandPolicy;
    private final OnboardingProvisioningService provisioningService;
    private final OrganizationUniquenessPolicy uniquenessPolicy;
    private final OnboardingApplicationViewAssembler viewAssembler;
    private final Clock clock;

    public ApplicationResult createApplication(ActorContext actor, CreateApplicationCommand command) {
        TenantType type = TenantType.parseSupported(command.type());
        commandPolicy.requireReadyEvidenceOwnedByPrincipal(actor.userId(), command.evidenceBundleId());

        OrganizationApplication application = switch (type) {
            case BRAND -> {
                uniquenessPolicy.assertUniqueBrand(command.orgName(), command.country(), command.bizRegNo());
                yield OrganizationApplication.createBrand(
                    actor.userId(), command.orgName(), command.country(),
                    command.bizRegNo(), command.address(), command.evidenceBundleId());
            }
            case RETAIL -> {
                uniquenessPolicy.assertUniqueRetail(command.orgName(), command.country(), command.bizRegNo());
                yield OrganizationApplication.createRetail(
                    actor.userId(), command.orgName(), command.country(),
                    command.bizRegNo(), command.address(), command.evidenceBundleId());
            }
            case SERVICE -> {
                uniquenessPolicy.assertUniqueService(command.orgName(), command.country(), command.bizRegNo());
                yield OrganizationApplication.createService(
                    actor.userId(), command.orgName(), command.country(),
                    command.bizRegNo(), command.address(), command.evidenceBundleId());
            }
            default -> throw new UserAuthDomainException(
                UserAuthErrorCode.INVALID_REQUEST,
                "Only BRAND, RETAIL, or SERVICE application type is supported"
            );
        };

        return viewAssembler.toResult(applicationRepository.save(application));
    }

    public ApproveApplicationResult approveApplication(ActorContext actor, String applicationId) {
        OrganizationApplication app = commandPolicy.findApplication(applicationId);
        app.assertPending();

        ProvisioningResult result = provisioningService.provision(
            app.type(), app.applicantUserId(), app.orgName(), app.country(), app.address(), actor.userId());

        app.approve(actor.userId(), result.tenantId(), Instant.now(clock));
        applicationRepository.save(app);

        return new ApproveApplicationResult(result.tenantId(), result.membershipId());
    }

    public ApplicationResult rejectApplication(ActorContext actor, String applicationId, String rejectReason) {
        OrganizationApplication app = commandPolicy.findApplication(applicationId);
        app.reject(actor.userId(), rejectReason, Instant.now(clock));
        return viewAssembler.toResult(applicationRepository.save(app));
    }
}
