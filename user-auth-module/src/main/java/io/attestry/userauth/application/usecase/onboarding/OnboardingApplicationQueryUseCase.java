package io.attestry.userauth.application.usecase.onboarding;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.view.ApplicationView;
import java.util.List;

public interface OnboardingApplicationQueryUseCase {

    List<ApplicationView> listApplications(String type);

    List<ApplicationView> listMyApplications(ActorContext actor);

    ApplicationView getMyApplication(ActorContext actor, String applicationId);

    ApplicationView getApplication(String applicationId);
}
