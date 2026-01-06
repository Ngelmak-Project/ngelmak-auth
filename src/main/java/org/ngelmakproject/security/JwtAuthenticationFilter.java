package org.ngelmakproject.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

  public JwtAuthenticationFilter(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
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
      // Long userId = Long.parseLong(optional.get().getSubject());
      String username = optional.get().get("username", String.class);
      String authorities = optional.get().get("authorities", String.class);

      // Extract authorities (roles) from custom claim
      List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities.split(","))
          .map(SimpleGrantedAuthority::new)
          .toList();

      // Create an Authentication object
      Authentication authentication = new UsernamePasswordAuthenticationToken(
          username, null, grantedAuthorities);

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
