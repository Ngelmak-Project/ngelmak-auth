package org.ngelmakproject.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.ngelmakproject.domain.User;
import org.ngelmakproject.domain.enumeration.CertificationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    String USERS_BY_LOGIN_CACHE = "usersByLogin";

    String USERS_BY_EMAIL_CACHE = "usersByEmail";
    Optional<User> findOneByActivationKey(String activationKey);
    Optional<User> findOneByResetKey(String resetKey);
    Optional<User> findOneByEmailIgnoreCase(String email);
    Optional<User> findOneByLogin(String login);
    Optional<User> findOneByDocId(String docId);
    Optional<User> findOneByDocIdAndCertificationStatusIn(String docId, CertificationStatus[] status);
    Optional<User> findOneByLoginAndCertificationStatus(String docId, CertificationStatus status);

    // Method to find non-activated users created before a certain date
    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);

    // Method to find users by certification status and created date
    List<User> findByCertificationStatusAndCreatedDateBefore(CertificationStatus status, Instant dateTime);

    @Modifying
    @Query("DELETE FROM User u WHERE u.createdDate < :dateTime AND u.certifiedDate = false")
    int deleteUnCertifiedUsersBeforeDate(@Param("dateTime") Instant dateTime);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesById(Long id);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String email);

}
