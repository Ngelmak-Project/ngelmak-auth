package org.ngelmakproject.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import org.ngelmakproject.domain.Authority;
import org.ngelmakproject.domain.ContactMessage;
import org.ngelmakproject.domain.User;
import org.ngelmakproject.repository.ContactMessageRepository;
import org.ngelmakproject.repository.UserRepository;
import org.ngelmakproject.security.AuthoritiesConstants;
import org.ngelmakproject.security.JwtUtil;
import org.ngelmakproject.web.rest.dto.LoginRequestDTO;
import org.ngelmakproject.web.rest.dto.RegisterRequestDTO;
import org.ngelmakproject.web.rest.errors.EmailAlreadyUsedException;
import org.ngelmakproject.web.rest.errors.LoginAlreadyUsedException;
import org.ngelmakproject.web.rest.errors.UserAlreadyActivatedException;
import org.ngelmakproject.web.rest.errors.UserBlockedException;
import org.ngelmakproject.web.rest.errors.UserNotActivatedException;
import org.ngelmakproject.web.rest.errors.UserNotFoundException;
import org.ngelmakproject.web.rest.util.RandomUtil;
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
     * Registers a new user in the system.
     *
     * <p>
     * This method performs the following key operations:
     * <ul>
     * <li>Validates that the login and email are unique</li>
     * <li>Creates a new user with an encrypted password</li>
     * <li>Sets initial user state as inactive</li>
     * <li>Generates an activation key</li>
     * <li>Assigns default user authority</li>
     * </ul>
     *
     * @param userDTO The data transfer object containing user registration details
     * @return The newly created and saved User entity
     *
     * @throws LoginAlreadyUsedException If the provided login is already in use
     * @throws EmailAlreadyUsedException If the provided email is already registered
     */
    public User register(RegisterRequestDTO userDTO) {
        // Log the registration request for debugging
        log.debug("Request to register a new User : {}", userDTO);

        // Check if login is already taken
        if (userRepository.findOneByLoginIgnoreCase(userDTO.login().toLowerCase()).isPresent()) {
            throw new LoginAlreadyUsedException();
        }

        // Check if email is already registered
        if (userRepository.findOneByEmailIgnoreCase(userDTO.email()).isPresent()) {
            throw new EmailAlreadyUsedException();
        }

        // Create a new user entity
        User newUser = new User();
        // Encrypt the user's password
        String encryptedPassword = passwordEncoder.encode(userDTO.password());
        // Set login (converted to lowercase to ensure uniqueness)
        newUser.setLogin(userDTO.login().toLowerCase());
        // Set email (converted to lowercase to ensure uniqueness)
        newUser.setEmail(userDTO.email().toLowerCase());
        // Set encrypted password
        newUser.setPassword(encryptedPassword);
        // Initially set user as inactive
        newUser.setActivated(false);
        // Generate a unique activation key
        newUser.setActivationKey(RandomUtil.generateKey());
        newUser.setActivationDate(Instant.now());
        // Assign default user authority
        var defaultAuthority = new Authority();
        defaultAuthority.setName(AuthoritiesConstants.USER);
        Set<Authority> authorities = Set.of(defaultAuthority);
        newUser.setAuthorities(authorities);
        // Save the new user to the database
        newUser = userRepository.save(newUser);

        log.debug("Created Information for User: {}\n With activation key: {}", newUser, newUser.getActivationKey());
        return newUser;
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * <p>
     * This method performs the following operations:
     * <ul>
     * <li>Looks up the user by login</li>
     * <li>Ensures the account is activated</li>
     * <li>Ensures the account is not blocked</li>
     * <li>Verifies the password</li>
     * <li>Generates a JWT token (standard or remember-me)</li>
     * </ul>
     *
     * @param loginRequestDTO Contains login credentials and remember-me preference
     * @return Optional containing the JWT token if authentication succeeds,
     *         or an empty Optional if authentication fails
     */
    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        log.debug("Request to authenticate a User: {}", loginRequestDTO);
        return userRepository.findOneByLoginIgnoreCase(loginRequestDTO.login())
                .map(user -> {
                    // Account must be activated
                    if (!user.isActivated()) {
                        throw new UserNotActivatedException();
                    }

                    // Account must not be blocked
                    if (user.isBlocked()) {
                        throw new UserBlockedException();
                    }

                    return user;
                })
                // Verify password
                .filter(user -> passwordEncoder.matches(loginRequestDTO.password(), user.getPassword()))
                // Generate token
                .map(user -> loginRequestDTO.rememberMe()
                        ? jwtUtil.generateRememberMeToken(user)
                        : jwtUtil.generateToken(user));
    }

    /**
     * Activates a user account using the provided activation key.
     *
     * <p>
     * This method performs the following steps:
     * <ul>
     * <li>Looks up a user associated with the given activation key.</li>
     * <li>Validates that the activation key has not expired
     * (maximum validity: 5 days from generation).</li>
     * <li>If valid, activates the user account and clears the activation key.</li>
     * <li>If the key is missing, invalid, or expired, a
     * {@link UserNotFoundException} is thrown.</li>
     * </ul>
     *
     * <p>
     * This ensures that activation links cannot be reused indefinitely and that
     * expired activation attempts fail securely.
     *
     * @param key the temporary activation key sent to the user
     * @return the activated user
     * @throws UserNotFoundException if the key is invalid, expired, or no user
     *                               matches it
     */
    public User activateUserByKey(String key) {
        log.debug("Activating user with key {}", key);
        return userRepository
                .findOneByActivationKey(key)
                .filter(user -> user.getActivationDate() != null &&
                        user.getActivationDate().isAfter(Instant.now().minus(5, ChronoUnit.DAYS)))
                .map(user -> {
                    user.setActivated(true);
                    user.setActivationKey(null);
                    user.setActivationDate(null);
                    log.debug("Activated user: {}", user);
                    return userRepository.save(user);
                })
                .orElseThrow(UserNotFoundException::new);
    }

    /**
     * Resends the activation email to the user with the specified email address.
     *
     * @param email the email address of the user
     * @throws UserAlreadyActivatedException if the user is already activated.
     */
    public void resendActivation(String email) {
        log.debug("Request to resend activation email for {}", email);
        userRepository
                .findOneByEmailIgnoreCase(email) // Only resend if the user exists and is not activated
                .ifPresent(user -> {
                    if (user.isActivated()) {
                        throw new UserAlreadyActivatedException();
                    }
                    boolean hasValidKey = user.getActivationKey() != null &&
                            user.getActivationDate() != null &&
                            user.getActivationDate().isAfter(Instant.now().minus(5, ChronoUnit.DAYS));

                    if (hasValidKey) {
                        log.debug("Reusing existing activation key {} for user {}", user.getActivationKey(), email);
                    } else {
                        user.setActivationKey(RandomUtil.generateKey());
                        user.setActivationDate(Instant.now());
                        log.debug("Generated new activation key for user {}: {}", email, user.getActivationKey());
                    }
                    userRepository.save(user);
                });
    }

    /**
     * Initiates a password reset request for the given user email.
     *
     * <p>
     * This method behaves as follows:
     * <ul>
     * <li>If the user exists and is activated, and an existing reset key is still
     * valid
     * (generated within the last 30 minutes), the same key is reused and its
     * validity
     * is effectively extended.</li>
     * <li>If no valid reset key exists, a new one is generated and
     * timestamped.</li>
     * <li>If the email does not correspond to an activated user, nothing
     * happens.</li>
     * </ul>
     *
     * <p>
     * This approach avoids generating multiple reset keys for repeated requests
     * within a
     * short period, prevents invalidating links the user may already have received,
     * and
     * ensures a consistent and user‑friendly reset flow.
     *
     * @param email the email address of the user requesting a password reset
     */
    public void requestPasswordReset(String email) {
        log.debug("Request to reset user with email {}", email);
        userRepository
                .findOneByEmailIgnoreCaseAndActivatedIsTrue(email)
                .ifPresent(user -> {
                    boolean hasValidKey = user.getResetKey() != null &&
                            user.getResetDate() != null &&
                            user.getResetDate().isAfter(Instant.now().minus(30, ChronoUnit.MINUTES));

                    if (hasValidKey) {
                        log.debug("Reusing existing reset key {} for user {}", user.getResetKey(), email);
                    } else {
                        user.setResetKey(RandomUtil.generateKey());
                        user.setResetDate(Instant.now());
                        log.debug("Generated new reset key for user {}: {}", email, user.getResetKey());
                    }

                    userRepository.save(user);
                });
    }

    /**
     * Completes password reset process for a user.
     * <p>
     * Resets password if the reset key is valid and not expired.
     *
     * @param key         Password reset key
     * @param newPassword New password to set
     */
    public void completePasswordReset(String key, String newPassword) {
        log.debug("Complete User password reset with key {}", key);
        userRepository
                .findOneByResetKey(key)
                .filter(user -> user.getResetDate().isAfter(Instant.now().minus(30, ChronoUnit.MINUTES)))
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setResetKey(null);
                    user.setResetDate(null);
                    return user;
                })
                .map(userRepository::save)
                .orElseThrow(UserNotFoundException::new);
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
