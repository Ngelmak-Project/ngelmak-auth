package org.ngelmakproject.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.ngelmakproject.domain.User;
import org.ngelmakproject.domain.enumeration.CertificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
@SuppressWarnings("unused")
public interface UserRepository extends JpaRepository<User, Long> {
	String USERS_BY_LOGIN_CACHE = "usersByLogin";

	String USERS_BY_EMAIL_CACHE = "usersByEmail";

	Optional<User> findOneByActivationKey(String activationKey);

	Optional<User> findOneByResetKey(String resetKey);

	Optional<User> findOneByEmailIgnoreCase(String email);

	Optional<User> findOneByEmailIgnoreCaseAndActivatedIsTrue(String email);

	Optional<User> findOneByEmailIgnoreCaseAndActivatedIsFalse(String email);

	Optional<User> findOneByLoginIgnoreCase(String login);

	Optional<User> findOneByEmailIgnoreCaseOrLoginIgnoreCase(String email, String login);

	Optional<User> findOneByDocId(String docId);

	Optional<User> findOneByDocIdAndCertificationStatusIn(String docId, CertificationStatus[] status);

	Optional<User> findOneByLoginAndCertificationStatus(String docId, CertificationStatus status);

	@EntityGraph(attributePaths = "authorities")
	@Query("SELECT u FROM User u")
	Page<User> findAllWithAuthorities(Pageable pageable);

	@EntityGraph(attributePaths = "authorities")
	@Query("SELECT u FROM User u WHERE " +
			"LOWER(u.login) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
			"LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
			"LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
			"LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
	Page<User> findAllUsersBySearchCriteria(@Param("search") String search, Pageable pageable);

	// Method to find non-activated users created before a certain date
	List<User> findAllByActivatedIsFalseAndCreatedDateBefore(Instant dateTime);

	// Method to find users by certification status and created date
	List<User> findByCertificationStatusAndCreatedDateBefore(CertificationStatus status, Instant dateTime);

	@Modifying
	@Query("DELETE FROM User u WHERE u.createdDate < :dateTime AND u.certificationStatus = :status")
	int deleteUnCertifiedUsersBeforeDate(@Param("dateTime") Instant dateTime,
			@Param("status") CertificationStatus status);

	/**
	 * Marks a user for deletion by setting the deletedDate field. This method
	 * performs a soft delete by updating the user's deletedDate instead of removing
	 * the record from the database.
	 * 
	 * @param id       The ID of the user to mark for deletion
	 * @param dateTime The timestamp to set as the deletedDate for the user
	 * @return The number of records updated (should be 1 if the user was
	 *         successfully marked for deletion, or 0 if the user was already marked
	 *         as deleted or does not exist)
	 */
	@Modifying
	@Query("UPDATE User u SET u.deletedDate = :dateTime WHERE u.id = :id")
	int markForDeletion(@Param("id") Long id, @Param("dateTime") Instant dateTime);

	// Method to find user IDs that are marked for deletion before a certain date.
	@Query("SELECT u.id FROM User u WHERE u.deletedDate < :dateTime")
	Set<Long> findIdsByDeletedDateBefore(@Param("dateTime") Instant dateTime);

	@EntityGraph(attributePaths = "authorities")
	Optional<User> findOneWithAuthoritiesById(Long id);

	@EntityGraph(attributePaths = "authorities")
	Optional<User> findOneWithAuthoritiesByLogin(String login);

	@EntityGraph(attributePaths = "authorities")
	Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String email);
}
