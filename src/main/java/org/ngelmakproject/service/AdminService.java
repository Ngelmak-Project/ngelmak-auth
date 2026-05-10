package org.ngelmakproject.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.ngelmakproject.domain.Authority;
import org.ngelmakproject.domain.AuthorityHistory;
import org.ngelmakproject.domain.ContactMessage;
import org.ngelmakproject.domain.ContactMessage.ContactStatus;
import org.ngelmakproject.domain.User;
import org.ngelmakproject.domain.enumeration.CertificationStatus;
import org.ngelmakproject.repository.AuthorityHistoryRepository;
import org.ngelmakproject.repository.AuthorityRepository;
import org.ngelmakproject.repository.AuthorityRequestRepository;
import org.ngelmakproject.repository.ContactMessageRepository;
import org.ngelmakproject.repository.UserRepository;
import org.ngelmakproject.web.rest.dto.CertificationDTO;
import org.ngelmakproject.web.rest.dto.PageDTO;
import org.ngelmakproject.web.rest.errors.ResourceNotFoundException;
import org.ngelmakproject.web.rest.errors.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private static final String ENTITY_NAME = "user";

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthorityRequestRepository authorityRequestRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final AuthorityRepository authorityRepository;
    private final AuthorityHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            AuthorityRepository authorityRepository,
            AuthorityHistoryRepository historyRepository,
            UserService userService,
            AuthorityRequestRepository authorityRequestRepository,
            ContactMessageRepository contactMessageRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.authorityRequestRepository = authorityRequestRepository;
        this.contactMessageRepository = contactMessageRepository;
        this.authorityRepository = authorityRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Sets the active status of a user.
     * 
     * @param id     of the user to update
     * @param active the new active status to set for the user
     * @return the updated User object with the new active status.
     */
    public User setActive(Long id, boolean active) {
        log.debug("Setting user active status to {} for user id {}", active, id);
        return userRepository
                .findById(id)
                .map(user -> {
                    user.setActivated(active);
                    user.setActivationKey(null);
                    log.debug("Activated user: {}", user);
                    return user;
                }).map(userRepository::save).orElseThrow(UserNotFoundException::new);
    }

    /**
     * Sets the block status of a user.
     * 
     * @param id      of the user to update
     * @param blocked the new block status to set for the user
     * @return the updated User object with the new block status.
     */
    public User updateBlockStatus(Long id, boolean blocked) {
        log.debug("Setting user block status to {} for user id {}", blocked, id);
        return userRepository
                .findById(id)
                .map(user -> {
                    user.setBlocked(blocked);
                    log.debug("Blocked user: {}", user);
                    return user;
                }).map(userRepository::save).orElseThrow(UserNotFoundException::new);
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
     * Certificate user account.
     *
     * @param certificationDTO user to update.
     * @return updated user.
     */
    public User certificate(CertificationDTO certificationDTO) {
        return this.userRepository.findById(certificationDTO.id())
                .map(existingUser -> {
                    existingUser.setDocType(certificationDTO.docType());
                    existingUser.setDocId(certificationDTO.docIdentification());
                    existingUser.setCertificationStatus(CertificationStatus.APPROVED);
                    existingUser.setCertifiedDate(Instant.now());
                    return existingUser;
                })
                .map(userRepository::save)
                .orElseThrow(UserNotFoundException::new);
    }

    /**
     * Grants authority to a user.
     *
     * @param id             of the user to update
     * @param authorityNames the names of the authorities to grant
     * @param reason         the reason for granting the authorities
     * @return the updated User object with the authorities granted.
     */
    public User grantAuthorities(Long id, Set<String> authorityNames, String reason) {
        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        // Conversion des noms → objets Authority
        Set<Authority> authorities = authorityRepository.findAll().stream()
                .filter(a -> authorityNames.contains(a.getName()))
                .collect(Collectors.toSet());

        return grantAuthorities(user, authorities, reason);
    }

    /**
     * Grants authority to a user.
     *
     * @param targetUser  the user to update
     * @param authorities the authorities to grant
     * @param reason      the reason for granting the authorities
     * @return the updated User object with the authorities granted.
     */
    public User grantAuthorities(User targetUser, Set<Authority> authorities, String reason) {
        User adminUser = userService.profile();

        // Ajout des autorités au user
        targetUser.getAuthorities().addAll(authorities);
        userRepository.save(targetUser);

        // Historisation
        List<AuthorityHistory> historyEntries = authorities.stream().map(auth -> {
            AuthorityHistory history = new AuthorityHistory();
            history.setUser(targetUser);
            history.setAuthority(auth);
            history.setActor(adminUser);
            history.setAction(AuthorityHistory.Action.APPROVED);
            history.setReason(reason);
            return history;
        }).toList();

        historyRepository.saveAll(historyEntries);

        return targetUser;
    }

    /**
     * Revokes authority from a user.
     *
     * @param id             of the user to update
     * @param authorityNames the names of the authorities to revoke
     * @param reason         the reason for revoking the authorities
     * @return the updated User object with the authorities revoked.
     */
    public User revokeAuthorities(Long id, Set<String> authorityNames, String reason) {
        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        Set<Authority> authoritiesToRemove = user.getAuthorities().stream()
                .filter(a -> authorityNames.contains(a.getName()))
                .collect(Collectors.toSet());

        return revokeAuthorities(user, authoritiesToRemove, reason);
    }

    /**
     * Revokes authority from a user.
     *
     * @param targetUser  the user to update
     * @param authorities the authorities to revoke
     * @param reason      the reason for revoking the authorities
     * @return the updated User object with the authorities revoked.
     */
    public User revokeAuthorities(User targetUser, Set<Authority> authorities, String reason) {
        User adminUser = userService.profile();

        targetUser.getAuthorities().removeAll(authorities);
        userRepository.save(targetUser);

        List<AuthorityHistory> historyEntries = authorities.stream().map(auth -> {
            AuthorityHistory history = new AuthorityHistory();
            history.setUser(targetUser);
            history.setAuthority(auth);
            history.setActor(adminUser);
            history.setAction(AuthorityHistory.Action.REVOKED);
            history.setReason(reason);
            return history;
        }).toList();

        historyRepository.saveAll(historyEntries);

        return targetUser;
    }

    /**
     * Handles an authority request by either approving or rejecting it based on the
     * provided parameters.
     *
     * @param requestId the ID of the authority request to handle
     * @param approve   a boolean indicating whether to approve (true) or reject
     *                  (false) the request
     * @param reason    the reason for approving or rejecting the request
     * @return the updated User object after handling the authority request
     * @throws ResourceNotFoundException if the authority request with the specified
     *                                   ID is not found
     */
    public User handleAuthorityRequest(Long requestId, boolean approve, String reason) {
        return authorityRequestRepository.findById(requestId)
                .map(existingRequest -> {
                    if (approve) {
                        // Grant the authority if approved
                        return grantAuthorities(existingRequest.getUser(), Set.of(existingRequest.getAuthority()),
                                reason);
                    } else {
                        // Revoke the authority if rejected (in case it was previously granted)
                        return revokeAuthorities(existingRequest.getUser(), Set.of(existingRequest.getAuthority()),
                                reason);
                    }
                })
                .orElseThrow(
                        () -> new ResourceNotFoundException("AuthorityRequest", ENTITY_NAME,
                                "authorityRequestNotFound"));
    }

    /**
     * Withdraws certification for a user.
     *
     * @param certificationDTO the DTO containing user ID and certification status.
     * @return the updated user.
     */
    public User certificationWithdrawal(Long id) {
        return this.userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setCertificationStatus(CertificationStatus.REJECTED);
                    existingUser.setDocType(null);
                    existingUser.setDocId(null);
                    existingUser.setCertifiedDate(null);
                    return existingUser;
                })
                .map(userRepository::save)
                .orElseThrow(UserNotFoundException::new);
    }

    /**
     * Get all the Contact Messages.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public PageDTO<ContactMessage> findAll(Pageable pageable) {
        log.debug("Request to get all ContactMessages");
        var page = contactMessageRepository.findAll(pageable);
        return PageDTO.from(page);
    }

    /**
     * Get all the untreated Contact Messages.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public PageDTO<ContactMessage> findAllUntreatedContactMessage(Pageable pageable) {
        log.debug("Request to get all ContactMessages");
        var page = contactMessageRepository.findUnclosedContactMessage(pageable);
        return PageDTO.from(page);
    }

    /**
     * Get all the contactMessages.
     *
     * @param id of the Message to close.
     * @return the closed Message.
     */
    public ContactMessage closeContactMessage(Long id) {
        log.debug("Request to get all ContactMessages");
        return contactMessageRepository.findById(id).map(existingMessage -> {
            existingMessage.setStatus(ContactStatus.CLOSED);
            return existingMessage;
        }).map(contactMessageRepository::save)
                .orElseThrow(() -> new ResourceNotFoundException("The message you try to close doesn't exist",
                        "contactMessage", "notFound"));
    }

    /**
     * Delete the contactMessage by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete ContactMessage : {}", id);
        contactMessageRepository.deleteById(id);
    }

    /**
     * Scheduled task to remove non-activated users who have been inactive for more
     * than 30 days.
     * 
     * <p>
     * This method runs every day at 3 AM and performs the following steps:
     * <ul>
     * <li>Queries the user repository for users that are not activated, have an
     * activation key, and a created date older than 30 days</li>
     * <li>Logs the IDs of the users that are being deleted</li>
     * <li>Deletes the identified users from the repository</li>
     * </ul>
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC") // every day at 3 AM
    public void removeNotActivatedUsers() {
        Set<Long> ids = userRepository
                .findAllByActivatedIsFalseAndCreatedDateBefore(
                        Instant.now().minus(30, ChronoUnit.DAYS))
                .stream()
                .map(user -> {
                    log.debug("Deleting not activated User {}", user);
                    return user.getId();
                }).collect(Collectors.toSet());
        if (!ids.isEmpty()) {
            userRepository.deleteAllById(ids);
        }
    }
}
