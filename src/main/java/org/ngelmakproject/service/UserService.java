package org.ngelmakproject.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
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
import org.ngelmakproject.web.rest.errors.UsernameAlreadyUsedException;
import org.ngelmakproject.web.rest.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
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
     * Get the connected user details.
     *
     * @return user details.
     */
    public UserPrincipal getUserWithAuthorities() {
        return ((UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal());
    }

    public Optional<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return userRepository
                .findOneByActivationKey(key)
                .map(user -> {
                    // activate given user for the registration key.
                    user.setActivated(true);
                    user.setActivationKey(null);
                    log.debug("Activated user: {}", user);
                    return user;
                });
    }

    public Optional<User> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);
        return userRepository
                .findOneByResetKey(key)
                .filter(user -> user.getResetDate().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)))
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setResetKey(null);
                    user.setResetDate(null);
                    return user;
                });
    }

    public Optional<User> requestPasswordReset(String mail) {
        return userRepository
                .findOneByEmailIgnoreCase(mail)
                .filter(User::isActivated)
                .map(user -> {
                    user.setResetKey(RandomUtil.generateResetKey());
                    user.setResetDate(Instant.now());
                    return user;
                });
    }

    public User registerUser(RegisterRequestDTO userDTO, String password) {
        userRepository
                .findOneByLogin(userDTO.getUsername().toLowerCase())
                .ifPresent(existingUser -> {
                    boolean removed = removeNonActivatedUser(existingUser);
                    if (!removed) {
                        throw new UsernameAlreadyUsedException();
                    }
                });
        User newUser = new User();
        String encryptedPassword = passwordEncoder.encode(password);
        newUser.setLogin(userDTO.getUsername().toLowerCase());
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        // new user is not active
        newUser.setActivated(false);
        // new user gets registration key
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        Set<Authority> authorities = new HashSet<>();
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent(authorities::add);
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);
        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    private boolean removeNonActivatedUser(User existingUser) {
        if (existingUser.isActivated()) {
            return false;
        }
        userRepository.delete(existingUser);
        userRepository.flush();
        return true;
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update.
     * @return updated user.
     */
    public Optional<UserDTO> updateUser(UserDTO userDTO) {
        return Optional.of(userRepository.findById(userDTO.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(user -> {
                    user.setLogin(userDTO.getLogin().toLowerCase());
                    user.setFirstName(userDTO.getFirstName());
                    user.setLastName(userDTO.getLastName());
                    if (userDTO.getEmail() != null) {
                        user.setEmail(userDTO.getEmail().toLowerCase());
                    }
                    user.setImageUrl(userDTO.getImageUrl());
                    user.setActivated(userDTO.isActivated());
                    user.setLangKey(userDTO.getLangKey());
                    Set<Authority> managedAuthorities = user.getAuthorities();
                    managedAuthorities.clear();
                    userDTO
                            .getAuthorities()
                            .stream()
                            .map(authorityRepository::findById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .forEach(managedAuthorities::add);
                    userRepository.save(user);
                    log.debug("Changed Information for User: {}", user);
                    return user;
                })
                .map(UserDTO::new);
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
     * Update basic information (first name, last name, email, language) for the
     * current user.
     *
     * @param firstName first name of user.
     * @param lastName  last name of user.
     * @param email     email id of user.
     * @param langKey   language key.
     */
    public void updateUser(String firstName, String lastName, String email, String langKey) {
        // SecurityUtils.getCurrentUserLogin()
        //         .flatMap(userRepository::findOneByLogin)
        //         .ifPresent(user -> {
        //             user.setFirstName(firstName);
        //             user.setLastName(lastName);
        //             if (email != null) {
        //                 user.setEmail(email.toLowerCase());
        //             }
        //             user.setLangKey(langKey);
        //             userRepository.save(user);
        //             log.debug("Changed Information for User: {}", user);
        //         });
    }

    @Transactional
    public void changePassword(String currentClearTextPassword, String newPassword) {
        // SecurityUtils.getCurrentUserLogin()
        //         .flatMap(userRepository::findOneByLogin)
        //         .ifPresent(user -> {
        //             String currentEncryptedPassword = user.getPassword();
        //             if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
        //                 throw new InvalidPasswordException();
        //             }
        //             String encryptedPassword = passwordEncoder.encode(newPassword);
        //             user.setPassword(encryptedPassword);
        //             log.debug("Changed password for User: {}", user);
        //         });
    }

    /**
     * Request for account certification.
     *
     * @param requestDTO user to update.
     * @return updated user.
     */
    // public Optional<RegisterRequestDTO> certificationRequest(AccountCertificationRequestDTO requestDTO) {
    //     return this.getUserWithAuthorities().map(
    //             user -> {
    //                 user.setCertificationStatus(CertificationStatus.REQUESTED);
    //                 user.setDocType(requestDTO.getDocType());
    //                 user.setDocId(requestDTO.getDocId());
    //                 userRepository.save(user);
    //                 log.debug("Changed Information for User: {}", user);
    //                 return user;
    //             })
    //             .map(RegisterRequestDTO::new);
    // }

    /**
     * Certificate user account.
     *
     * @param requestDTO user to update.
     * @return updated user.
     */
    // public Optional<RegisterRequestDTO> certificate(AccountCertificationRequestDTO requestDTO) {
    //     CertificationStatus[] status = { CertificationStatus.REJECTED, CertificationStatus.REQUESTED };
    //     return this.userRepository.findOneByDocIdAndCertificationStatusIn(requestDTO.getDocId(), status).map(
    //             user -> {
    //                 user.setDocId(
    //                         passwordEncoder.encode(requestDTO.getDocId()));
    //                 user.setCertificationStatus(CertificationStatus.CERTIFIED);
    //                 user.setDocType(requestDTO.getDocType());
    //                 user.setCertifiedDate(Instant.now());
    //                 userRepository.save(user);
    //                 log.debug("Changed Information for User: {}", user);
    //                 return user;
    //             })
    //             .map(RegisterRequestDTO::new);
    // }

    /**
     * Withdraw certification for the given user login.
     *
     * @param login user to update.
     * @return updated user.
     */
    // public Optional<RegisterRequestDTO> certificationWithdrawal(String login) {
    //     return this.userRepository.findOneByLoginAndCertificationStatus(login, CertificationStatus.CERTIFIED).map(
    //             user -> {
    //                 user.setDocId("");
    //                 user.setCertificationStatus(CertificationStatus.REJECTED);
    //                 user.setCertifiedDate(null);
    //                 userRepository.save(user);
    //                 log.debug("Changed Information for User: {}", user);
    //                 return user;
    //             })
    //             .map(RegisterRequestDTO::new);
    // }

    // @Transactional(readOnly = true)
    // public Optional<AccountCertificationRequestDTO> getAccountCertification(String login) {
    //     return this.userRepository.findOneByLogin(login.toLowerCase()).map(AccountCertificationRequestDTO::new);
    // }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDTO::new);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneWithAuthoritiesByLogin(login);
    }

    // @Transactional(readOnly = true)
    // public Optional<User> getUserWithAuthorities() {
    //     return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByLogin);
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
    //     log.debug("Request to update user image");
    //     return this.getUserWithAuthorities().map(
    //         user -> {
    //             /**
    //              * By default, user profile images are downloaded to the public directory, allowing access without authentication.
    //              * Then we get for instance /public/images/ngelmak/ngelmak-log.jpg
    //              */
    //             String[] dirs = { "media", "user" };
    //             URL url = fileStorageService.store(file, true, file.getOriginalFilename(), dirs);
    //                 user.setImageUrl(url.toString());
    //                 userRepository.save(user);
    //                 log.debug("Changed Information for User: {}", user);
    //                 return user;
    //             })
    //             .map(RegisterRequestDTO::new);
    // }
}
