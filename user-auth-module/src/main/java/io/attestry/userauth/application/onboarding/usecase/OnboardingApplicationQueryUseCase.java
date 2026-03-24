package io.attestry.userauth.application.onboarding.usecase;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.onboarding.view.ApplicationView;
import java.util.List;

public interface OnboardingApplicationQueryUseCase {

    List<ApplicationView> listApplications(String type);

    List<ApplicationView> listMyApplications(ActorContext actor);

    ApplicationView getMyApplication(ActorContext actor, String applicationId);

    ApplicationView getApplication(String applicationId);
}
