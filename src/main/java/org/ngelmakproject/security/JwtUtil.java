package org.ngelmakproject.security;

import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.ngelmakproject.domain.Authority;
import org.ngelmakproject.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Utility class for generating and validating JWT tokens using JJWT 0.9.1.
 */
@Component
public class JwtUtil {

  private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
  
  private final SecretKey secretKey;
  private final long expirationSeconds;
  private final long rememberMeExpirationSeconds;

  public JwtUtil(@Value("${jwt-secret-key}") String secret,
      @Value("${jwt-expiration-in-seconds}") long expirationSeconds,
      @Value("${jwt-expiration-in-seconds-for-remember-me}") long rememberMeExpirationSeconds) {
    this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    this.expirationSeconds = expirationSeconds;
    this.rememberMeExpirationSeconds = rememberMeExpirationSeconds;
  }

  /**
   * Generates a JWT token with subject and custom authorities claim.
   * 
   * @param username    Subject of the token (typically the user's username or
   *                    username)
   * @param authorities Set of roles or permissions
   * @return Signed JWT token as a String
   */
  public String buildToken(User user, long expirationSeconds) {
    long now = System.currentTimeMillis();
    return Jwts.builder().subject(user.getId().toString())
        .claim("username", user.getLogin())
        .claim("firstname", user.getFirstName())
        .claim("lastname", user.getLastName())
        .claim("email", user.getEmail())
        .claim("authorities", user.getAuthorities().stream().map(Authority::getName).collect(Collectors.joining(",")))
        .issuedAt(new Date(now)).expiration(new Date(now + expirationSeconds * 1000))
        .signWith(secretKey)
        .compact();
  }

  public String generateToken(User user) {
    return buildToken(user, expirationSeconds);
  }

  public String generateRememberMeToken(User user) {
    return buildToken(user, rememberMeExpirationSeconds);
  }

  /**
   * Validates the JWT token and returns the claims.
   * 
   * @param token JWT token to validate
   */
  public Claims validateToken(String token) {
    try {
      return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    } catch (JwtException e) {
      log.warn("JWT validation failed: {}", e.getMessage());
      throw e;
    }
  }

  public Optional<Claims> tryParseClaims(String token) {
    try {
      return Optional.of(validateToken(token));
    } catch (JwtException e) {
      return Optional.empty();
    }
  }
}