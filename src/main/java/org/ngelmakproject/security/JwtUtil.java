package org.ngelmakproject.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.ngelmakproject.domain.Authority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

/**
 * Utility class for generating and validating JWT tokens using JJWT 0.9.1.
 */
@Component
public class JwtUtil {

  private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

  private final SecretKey secretKey;

  /**
   * Initializes the secret key from a base64-encoded string in properties.
   */
  public JwtUtil(@Value("${jwt-secret-key:NOT_LOADED}") String secret) {
    // Decode base64 secret key
    byte[] keyBytes = Base64.getDecoder()
        .decode(secret.getBytes(StandardCharsets.UTF_8));
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * Generates a JWT token with subject and custom authorities claim.
   * 
   * @param username    Subject of the token (typically the user's username or
   *                    username)
   * @param authorities Set of roles or permissions
   * @return Signed JWT token as a String
   */
  public String generateToken(String username, Set<Authority> authorities) {
    return Jwts.builder()
        .subject(username)
        .claim("authorities", authorities.stream()
            .map(Authority::getName)
            .collect(Collectors.joining(" ")))
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
        .signWith(secretKey)
        .compact();
  }

  /**
   * Validates the JWT token and returns the claims.
   * 
   * @param token JWT token to validate
   * @throws JwtException if the token is invalid or expired
   */
  public Claims validateToken(String token) {
    log.debug("Validate the JWT token {}", token);
    try {
      return Jwts.parser().verifyWith(secretKey)
          .build()
          .parseSignedClaims(token).getPayload();
    } catch (SignatureException e) {
      throw new JwtException("Invalid JWT signature");
    } catch (JwtException e) {
      throw new JwtException("Invalid JWT");
    }
  }
}