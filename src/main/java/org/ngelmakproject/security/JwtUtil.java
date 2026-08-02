package org.ngelmakproject.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.ngelmakproject.domain.Authority;
import org.ngelmakproject.domain.User;
import org.ngelmakproject.web.rest.errors.TokenExpiredException;
import org.ngelmakproject.web.rest.errors.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

/**
 * Utility class for generating and validating JWT tokens using JJWT 0.9.1.
 */
@Component
public class JwtUtil {

  private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

  private final SecretKey secretKey;

  public JwtUtil(@Value("${jwt-secret-key}") String secret) {
    SecretKey key = null;

    try {
      byte[] decoded = Base64.getDecoder().decode(secret);
      key = Keys.hmacShaKeyFor(decoded);
    } catch (Exception ex) {
      // Beautiful log instead of exception
      log.error("❌ Invalid JWT secret key: the provided value is not valid Base64. " +
          "Please update 'jwt-secret-key' with a proper Base64-encoded value.");
    }

    this.secretKey = key;
  }

  /**
   * Generates a JWT token with subject and custom authorities claim.
   * 
   * @param username    Subject of the token (typically the user's username or
   *                    username)
   * @param authorities Set of roles or permissions
   * @return Signed JWT token as a String
   */
  public String generateAccessToken(User user) {
    long now = System.currentTimeMillis();
    return Jwts.builder().subject(user.getId().toString())
        .claim("login", user.getLogin())
        .claim("firstName", user.getFirstName())
        .claim("lastName", user.getLastName())
        .claim("email", user.getEmail())
        .claim("authorities", user.getAuthorities().stream().map(Authority::getName).collect(Collectors.joining(",")))
        // .issuedAt(new Date(now)).expiration(Date.from(Instant.now().plus(24, ChronoUnit.HOURS)))
        .issuedAt(new Date(now)).expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
        .signWith(secretKey)
        .compact();
  }

  /**
   * Validates the JWT token and returns the claims.
   * 
   * @param token JWT token to validate
   */
  public Claims validateToken(String token) {
    try {
      return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    } catch (ExpiredJwtException e) {
      throw new TokenExpiredException("Token has expired");
    } catch (SignatureException e) {
      throw new UnauthorizedException("Invalid signature");
    } catch (MalformedJwtException e) {
      throw new UnauthorizedException("Malformed token");
    } catch (UnsupportedJwtException e) {
      throw new UnauthorizedException("Unsupported token");
    } catch (IllegalArgumentException e) {
      throw new UnauthorizedException("Token is empty");
    } catch (JwtException e) {
      throw new UnauthorizedException("JWT processing error");
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