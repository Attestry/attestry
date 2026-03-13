package io.attestry.userauth.application.onboarding.query;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.view.ApplicationView;
import io.attestry.userauth.application.onboarding.assembler.OnboardingApplicationViewAssembler;
import io.attestry.userauth.application.port.onboarding.OrganizationApplicationRepositoryPort;
import io.attestry.userauth.application.usecase.onboarding.OnboardingApplicationQueryUseCase;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.onboarding.model.OrganizationApplication;
import io.attestry.userauth.domain.tenant.model.TenantType;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OnboardingApplicationQueryService implements OnboardingApplicationQueryUseCase {

    private final OrganizationApplicationRepositoryPort applicationRepository;
    private final OnboardingApplicationViewAssembler viewAssembler;


    @Override
    @Transactional(readOnly = true)
    public List<ApplicationView> listApplications(String type) {
        TenantType parsedType = TenantType.parseSupportedOrNull(type);
        List<OrganizationApplication> applications = parsedType == null
            ? applicationRepository.findAll()
            : applicationRepository.findByType(parsedType);

        return viewAssembler.toViews(applications);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationView> listMyApplications(ActorContext actor) {
        List<OrganizationApplication> applications = applicationRepository.findByApplicantUserId(actor.userId());
        return viewAssembler.toViews(applications);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationView getMyApplication(ActorContext actor, String applicationId) {
        OrganizationApplication application = applicationRepository
            .findByIdAndApplicantUserId(applicationId, actor.userId())
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
        return viewAssembler.toView(application);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationView getApplication(String applicationId) {
        OrganizationApplication application = applicationRepository
            .findById(applicationId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
        return viewAssembler.toView(application);
    }
}
