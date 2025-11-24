package org.ngelmakproject.config;

import org.ngelmakproject.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${ngelmak.file.upload-directory.location}")
    private String fileStorageLocation;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("api/authenticate").permitAll()
                        .requestMatchers("api/public/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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
}
