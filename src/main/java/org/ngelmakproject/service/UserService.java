package org.ngelmakproject.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.ngelmakproject.domain.Authority;
import org.ngelmakproject.domain.AuthorityRequest;
import org.ngelmakproject.domain.User;
import org.ngelmakproject.domain.enumeration.CertificationStatus;
import org.ngelmakproject.repository.AuthorityRequestRepository;
import org.ngelmakproject.repository.UserRepository;
import org.ngelmakproject.security.AuthoritiesConstants;
import org.ngelmakproject.security.UserPrincipal;
import org.ngelmakproject.web.rest.dto.AuthorityRequestDTO;
import org.ngelmakproject.web.rest.dto.CertificationDTO;
import org.ngelmakproject.web.rest.dto.RegisterRequestDTO;
import org.ngelmakproject.web.rest.dto.UserUpdateDTO;
import org.ngelmakproject.web.rest.errors.AuthorityNotFoundException;
import org.ngelmakproject.web.rest.errors.EmailAlreadyUsedException;
import org.ngelmakproject.web.rest.errors.InvalidPasswordException;
import org.ngelmakproject.web.rest.errors.LoginAlreadyUsedException;
import org.ngelmakproject.web.rest.errors.UserNotFoundException;
import org.ngelmakproject.web.rest.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AuthorityRequestRepository authorityRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            AuthorityRequestRepository authorityRequestRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authorityRequestRepository = authorityRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieves the conntected User details.
     *
     * <p>
     * This method is designed to be safe even when invoked in contexts where
     * authentication is not guaranteed (e.g., unsecured endpoints). It performs
     * several defensive checks to avoid runtime exceptions such as
     * {@link ClassCastException} or {@link NullPointerException}.
     * </p>
     *
     * @return an {@code Optional<UserPrincipal>} for the authenticated user, or
     *         empty
     *         if
     *         no valid authenticated user is present.
     */
    public Optional<UserPrincipal> getUserWithAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // No authentication available
        if (authentication == null) {
            return Optional.empty();
        }
        // Anonymous or not authenticated
        if (!authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        // Principal is not your expected custom user type
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            return Optional.empty();
        }
        // [TODO] Save the account if exists into cache.
        return Optional.of(userPrincipal);
    }

    /**
     * Retrieves the details of the currently logged-in user.
     * 
     * <p>
     * This method logs a debug message indicating that it is fetching the current
     * user's details. It retrieves the user's ID using the `getUserWithAuthorities`
     * method, and then queries the user repository
     * to find the corresponding User object. If the user is not found,
     * a `UserNotFoundException` is thrown.
     * 
     * @return the User object representing the current user, including their
     *         details and authorities
     * @throws UserNotFoundException if no user is found for the current session
     */
    public User profile() {
        log.debug("Get current User details");
        return getUserWithAuthorities()
                .map(UserPrincipal::id)
                .flatMap(userRepository::findById)
                .orElseThrow(UserNotFoundException::new);
    }

    /**
     * Activates a user account using the provided activation key.
     *
     * <p>
     * This method performs the following steps:
     * <ul>
     * <li>Looks up a user associated with the given activation key.</li>
     * <li>Validates that the activation key has not expired
     * (maximum validity: 24 hours from generation).</li>
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
                        user.getActivationDate().isAfter(Instant.now().minus(24, ChronoUnit.HOURS)))
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
     * Changes the user's password after validating the current password.
     * 
     * This method first retrieves the currently logged-in user through
     * the `getUserWithAuthorities` method. It then checks whether the
     * provided current plaintext password matches the user's existing
     * encrypted password. If it matches, the method encrypts the new
     * password and updates the user's password in the repository.
     * 
     * @param currentClearTextPassword the current password provided by the user in
     *                                 plaintext
     * @param newPassword              the new password to set for the user, also in
     *                                 plaintext
     * @throws InvalidPasswordException if the current provided password does not
     *                                  match the
     *                                  stored encrypted password
     */
    @Transactional
    public void changePassword(String currentClearTextPassword, String newPassword) {
        log.debug("Request change authenticated User's password");
        getUserWithAuthorities().map(UserPrincipal::id)
                .flatMap(userRepository::findById)
                .ifPresent(user -> {
                    String currentEncryptedPassword = user.getPassword();
                    if (!passwordEncoder.matches(currentClearTextPassword,
                            currentEncryptedPassword)) {
                        throw new InvalidPasswordException();
                    }
                    String encryptedPassword = passwordEncoder.encode(newPassword);
                    user.setPassword(encryptedPassword);
                    log.debug("Changed password for User: {}", user);
                });
    }

    /**
     * Updates the login for the currently authenticated user.
     *
     * @param login The new login to be set for the user
     * @return The updated User entity with the new login
     * @throws LoginAlreadyUsedException If the login is already taken by another
     *                                   user
     * @throws UserNotFoundException     If no user is found for the current
     *                                   authentication context
     */
    public User updateLogin(String login) {
        log.debug("Request to update User's login: {}", login);

        String normalizedLogin = login.toLowerCase();

        // Get current user principal
        UserPrincipal principal = getUserWithAuthorities()
                .orElseThrow(UserNotFoundException::new);

        // If the login is already the same, skip everything
        if (normalizedLogin.equals(principal.login())) {
            // No DB call needed — return the current user entity
            return userRepository.findById(principal.id())
                    .orElseThrow(UserNotFoundException::new);
        }

        // Check if login is used by another user
        userRepository.findOneByLoginIgnoreCase(normalizedLogin)
                .filter(other -> !other.getId().equals(principal.id()))
                .ifPresent(existing -> {
                    throw new LoginAlreadyUsedException();
                });

        // Load the user and update login
        return userRepository.findById(principal.id())
                .map(user -> {
                    user.setLogin(normalizedLogin);
                    log.debug("Updated login for User: {}", principal.id());
                    return user;
                })
                .orElseThrow(UserNotFoundException::new);
    }

    /**
     * Updates the email address for the currently authenticated user.
     *
     * @param email The new email address to be set for the user
     * @return The updated User entity with the new email address
     * @throws EmailAlreadyUsedException If the email is already registered by
     *                                   another user
     * @throws UserNotFoundException     If no user is found for the current
     *                                   authentication context
     */
    public User updateEmail(String email) {
        log.debug("Request to update User's email: {}", email);

        String normalizedEmail = email.toLowerCase();

        // Get current user principal
        UserPrincipal principal = getUserWithAuthorities()
                .orElseThrow(UserNotFoundException::new);

        // If the email is already the same, skip all checks and DB lookups
        if (normalizedEmail.equals(principal.email())) {
            return userRepository.findById(principal.id())
                    .orElseThrow(UserNotFoundException::new);
        }

        // Check if email is used by another user
        userRepository.findOneByEmailIgnoreCase(normalizedEmail)
                .filter(other -> !other.getId().equals(principal.id()))
                .ifPresent(existing -> {
                    throw new EmailAlreadyUsedException();
                });

        // Load the user and update email
        return userRepository.findById(principal.id())
                .map(user -> {
                    user.setEmail(normalizedEmail);
                    user.setActivated(false); // require reactivation
                    user.setActivationKey(RandomUtil.generateKey());
                    user.setActivationDate(Instant.now());
                    log.debug("Updated email for User: {}", principal.id());
                    return user;
                })
                .orElseThrow(UserNotFoundException::new);
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

        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    /**
     * Updates an existing User.
     * <p>
     * Only the fields that are present in the UserDTO will be updated.
     *
     * @param userDTO the data transfer object containing user details to update
     * @return an Optional containing the updated User, or an empty Optional if the
     *         user is not found
     * @throws UserNotFoundException if the User is not found in the repository
     */
    public User updateUser(UserUpdateDTO userUpdateDTO) {
        return this.getUserWithAuthorities()
                .map(UserPrincipal::id)
                .flatMap(userRepository::findById)
                .map(existingUser -> {
                    // Update user fields only if the value is present
                    if (userUpdateDTO.firstName() != null) {
                        existingUser.setFirstName(userUpdateDTO.firstName());
                    }
                    if (userUpdateDTO.lastName() != null) {
                        existingUser.setLastName(userUpdateDTO.lastName());
                    }
                    if (userUpdateDTO.langKey() != null) {
                        existingUser.setLangKey(userUpdateDTO.langKey());
                    }
                    if (userUpdateDTO.darkModeEnabled() != null) {
                        existingUser.setDarkModeEnabled(userUpdateDTO.darkModeEnabled());
                    }
                    // Save and return the updated user
                    return existingUser;
                })
                .map(userRepository::save)
                .orElseThrow(UserNotFoundException::new);
    }

    public User requestAuthority(String authorityName) {
        return this.getUserWithAuthorities()
                .map(UserPrincipal::id)
                .flatMap(userRepository::findById)
                .map(existingUser -> {
                    var authority = new Authority();
                    authority.setName(authorityName);
                    existingUser.getAuthorities().add(authority);
                    log.debug("Requested authority {} for User: {}", authorityName, existingUser);
                    return existingUser;
                })
                .map(userRepository::save)
                .orElseThrow(UserNotFoundException::new);
    }

    /**
     * Requests a new authority for the currently authenticated user with a provided
     * motivation.
     * This method creates a new AuthorityRequest entity that captures the user's
     * request for a specific authority along with their motivation. The request is
     * then saved to the database for further processing (e.g., admin review).
     * 
     * @param authorityName the name of the authority being requested.
     * @param motivation    the user's motivation for requesting the authority.
     * @return the created AuthorityRequest entity representing the user's request.
     * @throws AuthorityNotFoundException if the specified authority does not exist
     *                                    in the system.
     * @throws UserNotFoundException      if no user is found for the current
     *                                    authentication context.
     */
    public AuthorityRequest requestAuthority(String authorityName, String motivation) {
        User user = this.profile();
        Authority authority = AuthoritiesConstants.getAuthorities().stream()
                .filter(auth -> auth.equalsIgnoreCase(authorityName))
                .findFirst()
                .map(name -> {
                    Authority auth = new Authority();
                    auth.setName(name);
                    return auth;
                })
                .orElseThrow(() -> new AuthorityNotFoundException(authorityName));
        AuthorityRequest authorityRequest = new AuthorityRequest();
        authorityRequest.setUser(user); // Set the user making the request.
        authorityRequest.setMotivation(motivation);
        authorityRequest.setAuthority(authority);
        authorityRequest.setDecidedAt(Instant.now());
        authorityRequest = authorityRequestRepository.save(authorityRequest);
        return authorityRequest;
    }

    public List<AuthorityRequestDTO> getCurrentUserAuthorityRequests() {
        User user = this.profile();
        return authorityRequestRepository.findByUser(user).stream()
                .map(AuthorityRequestDTO::from)
                .toList();
    }

    /**
     * Requests certification for a user account.
     *
     * <p>
     * This method updates the user's certification status to PENDING and saves
     * the provided documentation details. The actual certification process (e.g.,
     * admin review) is expected to be handled separately.
     *
     * @param certificationDTO the DTO containing user ID and certification details
     */
    public void requestForCertification(CertificationDTO certificationDTO) {
        userRepository
                .findById(certificationDTO.id())
                .map(user -> {
                    // [TODO] Save the certification request into a separate table for auditing and
                    // tracking.
                    log.debug("Received certification request for User: {}", user);
                    user.setDocType(certificationDTO.docType());
                    user.setDocId(certificationDTO.docIdentification());
                    user.setCertificationStatus(CertificationStatus.PENDING);
                    return user;
                })
                .ifPresent(userRepository::save);
    }

    /**
     * Marks a user for deletion by setting a deletion timestamp.
     *
     * <p>
     * This method does not immediately delete the user from the database. Instead,
     * it sets a deletion timestamp, which can be used by a scheduled task to
     * perform actual deletion after a certain grace period. This approach allows
     * for potential recovery of user accounts and ensures that any related data can
     * be handled appropriately before permanent deletion.
     *
     * @param id the ID of the user to be marked for deletion
     */
    public void deleteUser(Long id) {
        userRepository
                .markForDeletion(id, Instant.now());
    }

    /**
     * Scheduled task to permanently delete users that have been marked for deletion
     * and have exceeded the grace period.
     *
     * <p>
     * This method runs at a fixed interval (e.g., daily) and checks for users that
     * have a deletion timestamp older than a specified cutoff date (e.g., 30 days).
     * It then permanently deletes those users from the database. This ensures that
     * user accounts are not immediately removed, allowing for potential recovery if
     * needed.
     */
    @Scheduled(cron = "0 0 3 * * ?") // Every day at 3 AM
    public void removeDeletedUsers() {
        Instant cutoffDate = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Long> ids = userRepository.findIdsByDeletedDateBefore(cutoffDate);
        if (!ids.isEmpty()) {
            log.debug("Deleting users with IDs: {}", ids);
            userRepository.deleteAllById(ids);
        }
    }

}
