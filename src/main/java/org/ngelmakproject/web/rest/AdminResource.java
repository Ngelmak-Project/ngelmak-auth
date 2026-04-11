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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing user accounts.
 * 
 * <p>
 * Base path: /api/admin/
 * 
 * - /api
 * - └── /admin # Admin-level secured endpoints
 * - │ ├── /users
 * - │ │ ├── GET / # List all users
 * - │ │ ├── GET /{id} # Get user details
 * - │ │ └── DELETE /{id} # Delete user
 * - │ │
 * - │ └── /management
 * - │ │ ├── GET /contacts # Get user feedback contacts
 * - │ │ ├── GET /audits
 * - │ │ ├── GET /logs
 * - │ │ └── GET /health
 */
@RestController
@RequestMapping("/api/admin")
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
	 * {@code GET /admin/users} : get all users with all the details - calling this
	 * are only allowed for the administrators.
	 *
	 * @param pageable the pagination information.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         all users.
	 */
	@GetMapping("/users")
	public ResponseEntity<PageDTO<User>> getAllUsers(Pageable pageable) {
		log.debug("REST request to get all User for an admin");
		Page<User> page = userRepository.findAllWithAuthorities(pageable);
		return ResponseEntity.ok().cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
				.body(PageDTO.from(page));
	}

	/**
	 * {@code GET /admin/users/:login} : get the "login" user.
	 *
	 * @param login the login of the user to find.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the "login" user, or with status {@code 404 (Not Found)}.
	 */
	@GetMapping("/users/{id}")
	public ResponseEntity<User> getUser(
			@PathVariable("id") Long id) {
		log.debug("REST request to get User : {}", id);
		return ResponseUtil.wrapOrNotFound(userRepository.findOneWithAuthoritiesById(id));
	}

	/**
	 * {@code PUT /admin/users/active} : change the active status of the user.
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
	 * {@code PUT /admin/users/grant-authorities} : grant authorities to a user.
	 *
	 * @param id             of the user to update
	 * @param authorityNames the names of the authorities to grant
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the updated user.
	 * @apiNote An email notification is sent to the user about the new authorities
	 *          via
	 *          {@link MailService#sendModeratorAcceptanceEmail(User)}
	 */
	@PutMapping("/users/grant-authorities")
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
	 * {@code PUT /admin/users/revoke-authorities} : revoke authorities from a user.
	 *
	 * @param id             of the user to update
	 * @param authorityNames the names of the authorities to revoke
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the updated user.
	 */
	@PutMapping("/users/revoke-authorities")
	public ResponseEntity<User> revokeAuthorities(@RequestBody GrantAuthorityDTO grantAuthorityDTO) {
		log.debug("REST request to revoke authorities {} from User : {}", grantAuthorityDTO.authorityNames(),
				grantAuthorityDTO.id());
		return ResponseEntity
				.ok(adminService.revokeAuthorities(grantAuthorityDTO.id(), grantAuthorityDTO.authorityNames(),
						grantAuthorityDTO.reason()));
	}

	/**
	 * {@code PUT /admin/users/authority-request} : handle authority request.
	 *
	 * @param id the id of the authority request to handle
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the updated user.
	 */
	@PutMapping("/users/authority-request")
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
	 * {@code PUT /admin/users/block} : block the user.
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
	 * {@code PUT /admin/users/unblock/:id} : unblock the user with the given id.
	 *
	 * @param login the login of the user to delete.
	 * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
	 */
	@PutMapping("/users/unblock/{id}")
	public ResponseEntity<User> unblockUser(@PathVariable("id") Long id) {
		log.debug("REST request to unblock User : {}", id);
		return ResponseEntity.ok(adminService.updateBlockStatus(id, false));
	}

	/**
	 * {@code Put /admin/users/certification} : requestion
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
	 * {@code Put /admin/users/certification/withdrawal} : withdrawal certification
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
	 * {@code GET /admin/management/contacts} : get all untreated contact messages.
	 *
	 * @param pageable the pagination information.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         all untreated contact messages.
	 */
	@GetMapping("/management/contacts")
	public ResponseEntity<PageDTO<ContactMessage>> getContactMessages(Pageable pageable) {
		log.debug("REST request to get all Contact Message for an admin");
		PageDTO<ContactMessage> page = adminService.findAll(pageable);
		return ResponseEntity.ok().cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
				.body(page);
	}

	private record CloseContactMessageDTO(Long id) {
	}

	/**
	 * {@code POST /admin/management/close} : close a contact messages.
	 *
	 * @param pageable the pagination information.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         of the updated contact message.
	 */
	@PostMapping("/management/close")
	public ResponseEntity<ContactMessage> closeContactMessages(@RequestBody CloseContactMessageDTO contactMessageDTO) {
		log.debug("REST request to close a Contact Message : {}", contactMessageDTO);
		ContactMessage message = adminService.closeContactMessage(contactMessageDTO.id);
		return ResponseEntity.ok().body(message);
	}
}