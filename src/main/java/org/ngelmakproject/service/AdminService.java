package org.ngelmakproject.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.ngelmakproject.domain.User;
import org.ngelmakproject.domain.enumeration.CertificationStatus;
import org.ngelmakproject.repository.UserRepository;
import org.ngelmakproject.web.rest.dto.CertificationDTO;
import org.ngelmakproject.web.rest.errors.UserNotFoundException;
import org.ngelmakproject.web.rest.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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
     * Scheduled task to remove non-certified users who have been inactive for more
     * than 7 days.
     * 
     * <p>
     * This method runs every day at 3 AM and performs the following steps:
     * <ul>
     * <li>Queries the user repository for users with a certification status of
     * NOT_REQUESTED and a created date older than 7 days</li>
     * <li>Logs the IDs of the users that are being deleted</li>
     * <li>Deletes the identified users from the repository</li>
     * </ul>
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC") // every day at 3 AM
    private void removeNonCertifiedUser() {
        List<Long> ids = userRepository
                .findByCertificationStatusAndCreatedDateBefore(CertificationStatus.NOT_REQUESTED,
                        Instant.now().minus(2, ChronoUnit.MONTHS))
                .stream()
                .map(user -> {
                    log.debug("Deleting non-certified User {}", user);
                    return user.getId();
                }).toList();
        if (!ids.isEmpty()) {
            // userRepository.deleteAllById(ids);
        }
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
    private void removeNotActivatedUsers() {
        List<Long> ids = userRepository
                .findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
                        Instant.now().minus(30, ChronoUnit.DAYS))
                .stream()
                .map(user -> {
                    log.debug("Deleting not activated User {}", user);
                    return user.getId();
                }).toList();
        if (!ids.isEmpty()) {
            // userRepository.deleteAllById(ids);
        }
    }
}
