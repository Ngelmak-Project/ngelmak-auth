package org.ngelmakproject.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.ngelmakproject.domain.Authority;
import org.ngelmakproject.domain.User;
import org.ngelmakproject.repository.AuthorityRepository;
import org.ngelmakproject.repository.UserRepository;
import org.ngelmakproject.security.AuthoritiesConstants;
import org.ngelmakproject.security.UserPrincipal;
import org.ngelmakproject.web.rest.dto.RegisterRequestDTO;
import org.ngelmakproject.web.rest.dto.UserDTO;
import org.ngelmakproject.web.rest.dto.UserUpdateDTO;
import org.ngelmakproject.web.rest.errors.EmailAlreadyUsedException;
import org.ngelmakproject.web.rest.errors.InvalidPasswordException;
import org.ngelmakproject.web.rest.errors.LoginAlreadyUsedException;
import org.ngelmakproject.web.rest.errors.UserNotFoundException;
import org.ngelmakproject.web.rest.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthorityRepository authorityRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
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
     * Marks the user as activated and clears the activation key.
     *
     * @param key Unique activation key
     * @return Optional of activated User
     */
    public Optional<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return userRepository
                .findOneByActivationKey(key)
                .map(user -> {
                    user.setActivated(true);
                    user.setActivationKey(null);
                    log.debug("Activated user: {}", user);
                    return user;
                }).map(userRepository::save);
    }

    /**
     * Completes password reset process for a user.
     * <p>
     * Resets password if the reset key is valid and not expired.
     *
     * @param newPassword New password to set
     * @param key         Password reset key
     * @return user with reset password
     */
    public User completePasswordReset(String newPassword, String key) {
        log.debug("Complete User password reset with key {}", key);
        return userRepository
                .findOneByResetKey(key)
                .filter(user -> user.getResetDate().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)))
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
     * Initiates password reset request for a user.
     * <p>
     * Generates a reset key for an activated user.
     *
     * @param email User's email address
     * @return Optional of user with reset key
     */
    public User requestPasswordReset(String email) {
        log.debug("Request to reset user with email {}", email);
        return userRepository
                .findOneByEmailIgnoreCase(email)
                .filter(User::isActivated)
                .map(user -> {
                    user.setResetKey(RandomUtil.generateResetKey());
                    user.setResetDate(Instant.now());
                    return user;
                })
                .map(userRepository::save)
                .orElseThrow(UserNotFoundException::new);
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
        if (userRepository.findOneByLogin(userDTO.login().toLowerCase()).isPresent()) {
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
        // Set encrypted password
        newUser.setPassword(encryptedPassword);
        // Initially set user as inactive
        newUser.setActivated(false);
        // Generate a unique activation key
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        // Assign default user authority
        var defaultAuthority = new Authority();
        defaultAuthority.setName(AuthoritiesConstants.USER);
        Set<Authority> authorities = Set.of(defaultAuthority);
        newUser.setAuthorities(authorities);

        // Save the new user to the database
        newUser = userRepository.save(newUser);

        // Log user creation for debugging
        log.debug("Created Information for User: {}", newUser);

        // Return the newly created user
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
                    // Save and return the updated user
                    return existingUser;
                })
                .map(userRepository::save)
                .orElseThrow(UserNotFoundException::new);
    }

    @Scheduled(cron = "0 0 3 * * *") // every day at 3 AM
    private void removeNonActivatedUser(User existingUser) {
        // if (existingUser.isActivated()) {
        // return false;
        // }
        // userRepository.delete(existingUser);
        // userRepository.flush();
        // return true;
    }

    public void deleteUser(String login) {
        userRepository
                .findOneByLogin(login)
                .ifPresent(user -> {
                    userRepository.delete(user);
                    log.debug("Deleted User: {}", user);
                });
    }
    /**
     * Request for account certification.
     *
     * @param requestDTO user to update.
     * @return updated user.
     */
    // public Optional<RegisterRequestDTO>
    // certificationRequest(AccountCertificationRequestDTO requestDTO) {
    // return this.getUserWithAuthorities().map(
    // user -> {
    // user.setCertificationStatus(CertificationStatus.REQUESTED);
    // user.setDocType(requestDTO.getDocType());
    // user.setDocId(requestDTO.getDocId());
    // userRepository.save(user);
    // log.debug("Changed Information for User: {}", user);
    // return user;
    // })
    // .map(RegisterRequestDTO::new);
    // }

    /**
     * Certificate user account.
     *
     * @param requestDTO user to update.
     * @return updated user.
     */
    // public Optional<RegisterRequestDTO>
    // certificate(AccountCertificationRequestDTO requestDTO) {
    // CertificationStatus[] status = { CertificationStatus.REJECTED,
    // CertificationStatus.REQUESTED };
    // return
    // this.userRepository.findOneByDocIdAndCertificationStatusIn(requestDTO.getDocId(),
    // status).map(
    // user -> {
    // user.setDocId(
    // passwordEncoder.encode(requestDTO.getDocId()));
    // user.setCertificationStatus(CertificationStatus.CERTIFIED);
    // user.setDocType(requestDTO.getDocType());
    // user.setCertifiedDate(Instant.now());
    // userRepository.save(user);
    // log.debug("Changed Information for User: {}", user);
    // return user;
    // })
    // .map(RegisterRequestDTO::new);
    // }

    /**
     * Withdraw certification for the given user login.
     *
     * @param login user to update.
     * @return updated user.
     */
    // public Optional<RegisterRequestDTO> certificationWithdrawal(String login) {
    // return this.userRepository.findOneByLoginAndCertificationStatus(login,
    // CertificationStatus.CERTIFIED).map(
    // user -> {
    // user.setDocId("");
    // user.setCertificationStatus(CertificationStatus.REJECTED);
    // user.setCertifiedDate(null);
    // userRepository.save(user);
    // log.debug("Changed Information for User: {}", user);
    // return user;
    // })
    // .map(RegisterRequestDTO::new);
    // }

    // @Transactional(readOnly = true)
    // public Optional<AccountCertificationRequestDTO>
    // getAccountCertification(String login) {
    // return
    // this.userRepository.findOneByLogin(login.toLowerCase()).map(AccountCertificationRequestDTO::new);
    // }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDTO::from);
    }

    // @Transactional(readOnly = true)
    // public Optional<User> getUserWithAuthorities() {
    // return
    // SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByLogin);
    // }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        userRepository
                .findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
                        Instant.now().minus(3, ChronoUnit.DAYS))
                .forEach(user -> {
                    log.debug("Deleting not activated user {}", user.getLogin());
                    userRepository.delete(user);
                });
    }

    /**
     * Gets a list of all the authorities.
     * 
     * @return a list of all the authorities.
     */
    @Transactional(readOnly = true)
    public List<String> getAuthorities() {
        return authorityRepository.findAll().stream().map(Authority::getName).toList();
    }

    /**
     * Save or update user image.
     *
     * @param attachment the entity to save.
     * @return the persisted entity.
     */
    // public Optional<RegisterRequestDTO> upload(MultipartFile file) {
    // log.debug("Request to update user image");
    // return this.getUserWithAuthorities().map(
    // user -> {
    // /**
    // * By default, user profile images are downloaded to the public directory,
    // allowing access without authentication.
    // * Then we get for instance /public/images/ngelmak/ngelmak-log.jpg
    // */
    // String[] dirs = { "media", "user" };
    // URL url = fileStorageService.store(file, true, file.getOriginalFilename(),
    // dirs);
    // user.setImageUrl(url.toString());
    // userRepository.save(user);
    // log.debug("Changed Information for User: {}", user);
    // return user;
    // })
    // .map(RegisterRequestDTO::new);
    // }
}
