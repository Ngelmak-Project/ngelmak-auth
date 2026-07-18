package org.ngelmakproject.config;

import org.ngelmakproject.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
/**
 * Enables method-level security annotations in Spring Security.
 * This allows fine-grained access control using:
 *
 * - @PreAuthorize: checks before method execution (e.g., roles, permissions)
 * - @PostAuthorize: checks after method execution (e.g., return value
 * filtering)
 * - @Secured: restricts access based on roles (e.g., @Secured("ROLE_ADMIN"))
 * - @RolesAllowed: JSR-250 annotation for role-based access
 * (e.g., @RolesAllowed("ROLE_USER"))
 *
 * Requires Spring Security to inject method-level interceptors.
 */
@EnableMethodSecurity
public class SpringSecurityConfig {

    /**
     * Defines the list of public (unauthenticated) API endpoints.
     *
     * This RequestMatcher is shared between:
     * - the SecurityFilterChain (to mark these endpoints as permitAll)
     * - the JwtAuthenticationFilter (to skip JWT validation on these endpoints)
     *
     * Keeping this list in a single bean avoids duplication and ensures
     * consistent behavior across the security layer.
     */
    @Bean
    public RequestMatcher publicEndpointsMatcher() {
        return new OrRequestMatcher(
                new AntPathRequestMatcher("/api/v1/login"),
                new AntPathRequestMatcher("/api/v1/register"),
                new AntPathRequestMatcher("/api/v1/activate"),
                new AntPathRequestMatcher("/api/v1/activate/resend"),
                new AntPathRequestMatcher("/api/v1/password-reset"),
                new AntPathRequestMatcher("/api/v1/password-reset/confirm"),
                new AntPathRequestMatcher("/api/v1/contact"),
                new AntPathRequestMatcher("/api/v1/donations/stats"),
                new AntPathRequestMatcher("/api/v1/donations/recent"));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RequestMatcher publicEndpointsMatcher,
            JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicEndpointsMatcher).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * An instance of BCryptPasswordEncoder, which hashes passwords using the BCrypt
     * algorithm
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("user"));
    }
}