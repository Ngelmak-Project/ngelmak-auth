package org.ngelmakproject.service;

import java.time.Instant;
import java.util.Optional;

import org.ngelmakproject.domain.ContactMessage;
import org.ngelmakproject.repository.ContactMessageRepository;
import org.ngelmakproject.repository.UserRepository;
import org.ngelmakproject.security.JwtUtil;
import org.ngelmakproject.web.rest.dto.LoginRequestDTO;
import org.ngelmakproject.web.rest.errors.UserBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;

/**
 * Service class for managing users.
 */
@Service
public class AuthenticateService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticateService.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContactMessageRepository contactMessageRepository;

    public AuthenticateService(JwtUtil jwtUtil, UserRepository userRepository, PasswordEncoder passwordEncoder,
            ContactMessageRepository contactMessageRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.contactMessageRepository = contactMessageRepository;
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * <p>
     * This method performs the following key operations:
     * <ul>
     * <li>Looks up user by username</li>
     * <li>Ensures user is not blocked</li>
     * <li>Verifies password using secure password matching</li>
     * <li>Generates either a standard or remember-me JWT token</li>
     * </ul>
     *
     * @param loginRequestDTO Contains login credentials and remember-me preference
     * @return Optional containing the JWT token if authentication is successful,
     *         or an empty Optional if authentication fails
     *
     * @see LoginRequestDTO
     * @see JwtUtil
     */
    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        // Log authentication attempt for debugging
        log.debug("Request to authenticate a User : {}", loginRequestDTO);

        // Complex authentication flow using method chaining
        Optional<String> token = userRepository.findOneByLoginIgnoreCase(loginRequestDTO.login())
                .map(u -> {
                    // Check if the user account is blocked before proceeding.
                    if (u.isBlocked()) {
                        throw new UserBlockedException();
                    }
                    return u;
                })
                // Filter: Verify password using secure encoder
                .filter(u -> passwordEncoder.matches(loginRequestDTO.password(), u.getPassword()))
                // Map: Generate token based on remember-me preference
                .map(u -> loginRequestDTO.rememberMe()
                        ? jwtUtil.generateRememberMeToken(u) // Long-lived token
                        : jwtUtil.generateToken(u)); // Standard token

        return token;
    }

    /**
     * Validates a JWT token's integrity and expiration.
     *
     * <p>
     * This method provides a safe way to check token validity with:
     * <ul>
     * <li>Exception handling for token validation</li>
     * <li>Graceful failure without throwing exceptions</li>
     * </ul>
     *
     * @param token The JWT token to validate
     * @return boolean indicating whether the token is valid
     *
     * @see JwtUtil
     */
    public boolean validateToken(String token) {
        try {
            // Attempt to validate the token
            jwtUtil.validateToken(token);
            return true;
        } catch (JwtException e) {
            // Log the validation failure (optional, depends on logging strategy)
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Handles contact support messages by saving them to the database.
     *
     * <p>
     * This method performs the following operations:
     * <ul>
     * <li>Logs the incoming contact message for debugging</li>
     * <li>Creates a new ContactMessage entity with the provided details</li>
     * <li>Sets the status to NEW and timestamps the creation time</li>
     * <li>Saves the ContactMessage to the database using the repository</li>
     * </ul>
     *
     * @param name    The name of the user contacting support (optional)
     * @param email   The email address of the user contacting support (optional)
     * @param subject The subject of the support message
     * @param message The body of the support message
     */
    public void contactSupport(String name, String email, String subject, String message) {
        log.debug("Received contact support message from {}: {}", email, subject);
        ContactMessage contactMessage = new ContactMessage();
        contactMessage.setName(name);
        contactMessage.setEmail(email);
        contactMessage.setSubject(subject);
        contactMessage.setMessage(message);
        contactMessage.setSentAt(Instant.now());
        contactMessage.setStatus(ContactMessage.ContactStatus.NEW);
        contactMessageRepository.save(contactMessage);
    }

}
