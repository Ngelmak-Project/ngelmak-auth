package org.ngelmakproject.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom filter that intercepts HTTP requests to validate JWT tokens using
 * JJWT.
 * If the token is valid, it sets the authenticated user into Spring Security's
 * context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final JwtUtil jwtUtil;
  private final RequestMatcher publicEndpointsMatcher;

  public JwtAuthenticationFilter(JwtUtil jwtUtil, RequestMatcher publicEndpointsMatcher) {
    this.jwtUtil = jwtUtil;
    this.publicEndpointsMatcher = publicEndpointsMatcher;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return publicEndpointsMatcher.matches(request);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    log.info("🔐 JWT filter triggered");

    // Extract the Authorization header
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    // If no token is provided, continue without authentication
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    // Remove "Bearer " prefix to get the token
    String token = authHeader.substring(7);

    // Parse and validate the JWT token
    Optional<Claims> optional = jwtUtil.tryParseClaims(token);
    if (optional.isPresent()) {
      String userId = optional.get().getSubject();
      String login = optional.get().get("login", String.class);
      String firstName = optional.get().get("firstName", String.class);
      String lastName = optional.get().get("lastName", String.class);
      String email = optional.get().get("email", String.class);
      String authoritiesStr = optional.get().get("authorities", String.class);

      log.info("\n" +
          "========< Gateway Auth Filter >=========\n" +
          "User-Id          : {}\n" +
          "User-Login       : {}\n" +
          "User-Firstname   : {}\n" +
          "User-Lastname    : {}\n" +
          "User-Email       : {}\n" +
          "User-Authorities : {}\n" +
          "========================================", userId, login, firstName, lastName, email, authoritiesStr);

      // Extract authorities (roles) from custom claim
      Set<String> roles = Arrays.stream(authoritiesStr.split(","))
          .collect(Collectors.toSet());

      List<SimpleGrantedAuthority> grantedAuthorities = roles.stream()
          .map(SimpleGrantedAuthority::new)
          .toList();

      UserPrincipal principal = new UserPrincipal(Long.parseLong(userId), login, firstName, lastName, email,
          roles);

      // Create an Authentication object
      Authentication authentication = new UsernamePasswordAuthenticationToken(
          principal, null, grantedAuthorities);

      // Inject the Authentication into Spring Security's context
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } else {
      // If token is invalid, respond with 401 Unauthorized
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    // Continue the filter chain
    filterChain.doFilter(request, response);
  }
}
