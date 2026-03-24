package io.attestry.security;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.product.application.common.ProductActor;
import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentActorArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentActor.class)
            && (parameter.getParameterType().equals(ActorContext.class)
            || parameter.getParameterType().equals(ProductActor.class));
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new IllegalStateException("No AuthPrincipal found in SecurityContext");
        }
        if (parameter.getParameterType().equals(ProductActor.class)) {
            return new ProductActor(
                principal.userId(),
                principal.tenantId(),
                principal.scopes(),
                principal.scopes() != null && principal.scopes().contains(PermissionCodes.PLATFORM_ADMIN)
            );
        }
        return ActorContext.from(principal);
    }
}
