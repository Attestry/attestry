package io.attestry.security;

import io.attestry.commonlib.domain.exception.DomainException;
import io.attestry.userauth.application.port.auth.AccessTokenPort;
import io.attestry.userauth.interfaces.auth.BearerTokenExtractor;
import io.attestry.userauth.security.AuthPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

    private final AccessTokenPort accessTokenPort;

    public AccessTokenAuthenticationFilter(AccessTokenPort accessTokenPort) {
        this.accessTokenPort = accessTokenPort;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/auth/signup")
            || path.equals("/auth/login")
            || path.equals("/auth/signup/email-verifications")
            || path.equals("/auth/signup/email-verifications/confirm")
            || path.equals("/api-v1/auth/signup")
            || path.equals("/api-v1/auth/login")
            || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = BearerTokenExtractor.extract(authorization);
            AuthPrincipal principal = accessTokenPort.parse(token).orElse(null);
            if (principal != null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    token,
                    authorities(principal)
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (DomainException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"ACCESS_TOKEN_INVALID\",\"message\":\"Invalid access token\"}");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private List<SimpleGrantedAuthority> authorities(AuthPrincipal principal) {
        return principal.scopes().stream()
            .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
            .toList();
    }
}
