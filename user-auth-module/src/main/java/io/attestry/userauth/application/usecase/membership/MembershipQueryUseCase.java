package io.attestry.userauth.application.usecase.membership;

import io.attestry.userauth.application.dto.view.MembershipView;
import java.util.List;

public interface MembershipQueryUseCase {
    List<MembershipView> getMemberships(String userId);
}
