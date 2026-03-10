package io.attestry.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class UserAuthSecurityConfig {

    @Bean
    public SecurityFilterChain userAuthFilterChain(HttpSecurity http, AccessTokenAuthenticationFilter accessTokenFilter)
        throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/signup", "/auth/login", "/api-v1/auth/signup", "/api-v1/auth/login", "/error")
                .permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/products/passports/*",
                    "/products/passports/*/state",
                    "/products/passports/*/owner",
                    "/ledgers/passports/*/entries",
                    "/ledgers/passports/*/entries/*",
                    "/ledgers/passports/*/verify",
                    "/workflows/service/providers",
                    "/actuator/health",
                    "/actuator/health/*",
                    "/actuator/info",
                    "/actuator/metrics",
                    "/actuator/metrics/*",
                    "/actuator/prometheus",
                    "/actuator/prometheus/*")
                .permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(accessTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Access denied\"}");
                }))
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
