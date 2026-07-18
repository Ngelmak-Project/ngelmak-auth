package org.ngelmakproject.web.rest;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.ngelmakproject.domain.ContactMessage;
import org.ngelmakproject.domain.User;
import org.ngelmakproject.repository.UserRepository;
import org.ngelmakproject.security.AuthoritiesConstants;
import org.ngelmakproject.service.AdminService;
import org.ngelmakproject.service.email.MailService;
import org.ngelmakproject.web.rest.dto.ActiveUserDTO;
import org.ngelmakproject.web.rest.dto.CertificationDTO;
import org.ngelmakproject.web.rest.dto.PageDTO;
import org.ngelmakproject.web.rest.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Management API
 *
 * <p>
 * Base path: /api/v1/admin
 *
 * <p>
 * Restricted endpoints for administrative operations: user management,
 * authority/certification handling, user blocking, and support contact
 * inquiries.
 *
 * <p>
 * Requires: Authentication + Admin role
 *
 * <h3>User Management</h3>
 * <ul>
 * <li>{@code GET /users} - List all users with pagination</li>
 * <li>{@code GET /users/:id} - Get user details by ID</li>
 * <li>{@code PUT /users/active} - Set user active/inactive status</li>
 * </ul>
 *
 * <h3>User Authorities & Certification</h3>
 * <ul>
 * <li>{@code PUT /users/authorities/grant} - Grant authorities to user</li>
 * <li>{@code PUT /users/authorities/revoke} - Revoke authorities from user</li>
 * <li>{@code PUT /users/authorities/requests} - Approve/reject authority
 * requests</li>
 * <li>{@code PUT /users/certification} - Certify user account</li>
 * <li>{@code PUT /users/certification/withdrawal/:id} - Withdraw user
 * certification</li>
 * </ul>
 *
 * <h3>User Blocking</h3>
 * <ul>
 * <li>{@code PUT /users/block} - Block/suspend user account</li>
 * <li>{@code PUT /users/unblock} - Unblock user account</li>
 * </ul>
 *
 * <h3>Support Management</h3>
 * <ul>
 * <li>{@code GET /contacts} - List contact message submissions</li>
 * <li>{@code PUT /contacts/close/:id} - Close contact message</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class AdminResource {
	private static final Logger log = LoggerFactory.getLogger(AdminResource.class);

	@Value("${spring.application.name}")
	private String applicationName;

	// DTO for handling granting/revoking authorities
	private record GrantAuthorityDTO(Long id, Set<String> authorityNames, String reason) {
	}

	// DTO for handling authority requests
	private record AccessApprovalDTO(Long id, boolean approve, String reason) {
	}

	private final AdminService adminService;
	private final UserRepository userRepository;
	private final MailService mailService;

	public AdminResource(AdminService adminService, UserRepository userRepository, MailService mailService) {
		this.adminService = adminService;
		this.userRepository = userRepository;
		this.mailService = mailService;
	}

	/**
	 * {@code GET /users} : get all users with all the details - calling this
	 * are only allowed for the administrators.
	 *
	 * @param pageable the pagination information.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         all users.
	 */
	@GetMapping("/users")
	public ResponseEntity<PageDTO<User>> getAllUsers(
			@RequestParam(value = "q", defaultValue = "") String query,
			Pageable pageable) {
		log.debug("REST request to get all User for an admin");
		Page<User> page = query.isBlank() ? userRepository.findAllWithAuthorities(pageable)
				: userRepository.findAllUsersBySearchCriteria(query, pageable);
		return ResponseEntity.ok().cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
				.body(PageDTO.from(page));
	}

	/**
	 * {@code GET /users/:id} : get the "id" user.
	 *
	 * @param id the id of the user to find.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the "id" user, or with status {@code 404 (Not Found)}.
	 */
	@GetMapping("/users/{id}")
	public ResponseEntity<User> getUser(@PathVariable("id") Long id) {
		log.debug("REST request to get User : {}", id);
		return ResponseUtil.wrapOrNotFound(userRepository.findOneWithAuthoritiesById(id));
	}

	/**
	 * {@code PUT /users/active} : change the active status of the user.
	 *
	 * @param activeUserDTO the id and the new active status of the user.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the updated user.
	 */
	@PutMapping("/users/active")
	public ResponseEntity<User> setActive(@RequestBody ActiveUserDTO activeUserDTO) {
		log.debug("REST request to change active status of User : {} to {}", activeUserDTO.id(),
				activeUserDTO.isActive());
		return ResponseEntity.ok(adminService.setActive(activeUserDTO.id(), activeUserDTO.isActive()));
	}

	/**
	 * {@code PUT /users/authorities/grant} : grant authorities to a user.
	 *
	 * @param id             of the user to update
	 * @param authorityNames the names of the authorities to grant
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the updated user.
	 * @apiNote An email notification is sent to the user about the new authorities
	 *          via
	 *          {@link MailService#sendModeratorAcceptanceEmail(User)}
	 */
	@PutMapping("/users/authorities/grant")
	public ResponseEntity<User> grantAuthorities(@RequestBody GrantAuthorityDTO grantAuthorityDTO) {
		log.debug("REST request to grant authorities {} to User : {} with reason {}",
				grantAuthorityDTO.authorityNames(),
				grantAuthorityDTO.id(), grantAuthorityDTO.reason());
		User user = adminService.grantAuthorities(grantAuthorityDTO.id(), grantAuthorityDTO.authorityNames(),
				grantAuthorityDTO.reason());
		if (grantAuthorityDTO.authorityNames().contains(AuthoritiesConstants.MODERATOR)) {
			mailService.sendModeratorAcceptanceEmail(user);
		}
		return ResponseEntity.ok(user);
	}

	/**
	 * {@code PUT /users/authorities/revoke} : revoke authorities from a user.
	 *
	 * @param id             of the user to update
	 * @param authorityNames the names of the authorities to revoke
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the updated user.
	 */
	@PutMapping("/users/authorities/revoke")
	public ResponseEntity<User> revokeAuthorities(@RequestBody GrantAuthorityDTO grantAuthorityDTO) {
		log.debug("REST request to revoke authorities {} from User : {}", grantAuthorityDTO.authorityNames(),
				grantAuthorityDTO.id());
		return ResponseEntity
				.ok(adminService.revokeAuthorities(grantAuthorityDTO.id(), grantAuthorityDTO.authorityNames(),
						grantAuthorityDTO.reason()));
	}

	/**
	 * {@code PUT /users/authorities/requests} : handle authority request.
	 *
	 * @param id the id of the authority request to handle
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the updated user.
	 */
	@PutMapping("/users/authorities/requests")
	public ResponseEntity<User> handleAuthorityRequest(
			@RequestBody AccessApprovalDTO authorityRequestDTO) {
		log.debug("REST request to handle authority request for User : {} with approve {} and reason {}",
				authorityRequestDTO.id(), authorityRequestDTO.approve(), authorityRequestDTO.reason());
		return ResponseEntity
				.ok(adminService.handleAuthorityRequest(authorityRequestDTO.id(), authorityRequestDTO.approve(),
						authorityRequestDTO.reason()));
	}

	private record BlockUserDTO(Long id, String duration, String reason, String contentType,
			Long appealId) {
	}

	/**
	 * {@code PUT /users/block} : block the user.
	 *
	 * @param blockUserDTO the DTO containing the user ID and suspension details.
	 * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
	 */
	@PutMapping("/users/block")
	public ResponseEntity<User> blockUser(@RequestBody BlockUserDTO blockUserDTO) {
		log.debug("REST request to block User : {}", blockUserDTO.id());
		User user = adminService.updateBlockStatus(blockUserDTO.id(), true);
		mailService.sendSuspensionNoticeEmail(user, blockUserDTO.duration(), blockUserDTO.reason(),
				blockUserDTO.contentType(), blockUserDTO.appealId());
		return ResponseEntity.ok(user);
	}

	/**
	 * {@code PUT /users/unblock/:id} : unblock the user with the given id.
	 *
	 * @param id the id of the user to unblock.
	 * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
	 */
	@PutMapping("/users/unblock/{id}")
	public ResponseEntity<User> unblockUser(@PathVariable("id") Long id) {
		log.debug("REST request to unblock User : {}", id);
		return ResponseEntity.ok(adminService.updateBlockStatus(id, false));
	}

	/**
	 * {@code Put /users/certification} : requestion
	 * certification for a user account.
	 *
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the account just certified.
	 */
	@PutMapping("/users/certification")
	public ResponseEntity<User> certificateUser(@RequestBody CertificationDTO certificationDTO) {
		log.debug("REST request to certify User : {}", certificationDTO);
		return ResponseEntity.ok(adminService.certificate(certificationDTO));
	}

	/**
	 * {@code Put /users/certification/withdrawal/:id} : withdrawal
	 * certification
	 * for a user account.
	 *
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the account just withdrawn.
	 */
	@PutMapping("/users/certification/withdrawal/{id}")
	public ResponseEntity<User> certificationWithdrawal(@PathVariable("id") Long id) {
		log.debug("REST request to withdraw certification of User : {}", id);
		return ResponseEntity.ok(adminService.certificationWithdrawal(id));
	}

	/**
	 * {@code GET /contacts} : get all untreated contact messages.
	 *
	 * @param pageable the pagination information.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         all untreated contact messages.
	 */
	@GetMapping("/contacts")
	public ResponseEntity<PageDTO<ContactMessage>> getContactMessages(Pageable pageable) {
		log.debug("REST request to get all Contact Message for an admin");
		PageDTO<ContactMessage> page = adminService.findAll(pageable);
		return ResponseEntity.ok().cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
				.body(page);
	}

	/**
	 * {@code PUT /contacts/close/:id} : close a contact messages.
	 *
	 * @param pageable the pagination information.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         of the updated contact message.
	 */
	@PutMapping("/contacts/close/{id}")
	public ResponseEntity<ContactMessage> closeContactMessages(@PathVariable("id") Long id) {
		log.debug("REST request to close a Contact Message : {}", id);
		ContactMessage message = adminService.closeContactMessage(id);
		return ResponseEntity.ok().body(message);
	}
}