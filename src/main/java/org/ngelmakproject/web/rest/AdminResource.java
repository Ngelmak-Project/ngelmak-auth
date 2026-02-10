package org.ngelmakproject.web.rest;

import java.util.concurrent.TimeUnit;

import org.ngelmakproject.domain.User;
import org.ngelmakproject.repository.UserRepository;
import org.ngelmakproject.security.AuthoritiesConstants;
import org.ngelmakproject.service.AdminService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing user accounts.
 * /api
 * └── /admin # Admin-level secured endpoints
 * │ ├── /users
 * │ │ ├── GET / # List all users
 * │ │ ├── GET /{id} # Get user details
 * │ │ └── DELETE /{id} # Delete user
 * │ │
 * │ └── /management
 * │ │ ├── GET /audits
 * │ │ ├── GET /logs
 * │ │ └── GET /health
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class AdminResource {
	private static final Logger log = LoggerFactory.getLogger(AdminResource.class);

	@Value("${spring.application.name}")
	private String applicationName;

	private final AdminService adminService;
	private final UserRepository userRepository;

	public AdminResource(AdminService adminService, UserRepository userRepository) {
		this.adminService = adminService;
		this.userRepository = userRepository;
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
		Page<User> page = userRepository.findAll(pageable);
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
		log.debug("REST request to change active status of User : {} to {}", activeUserDTO.id(), activeUserDTO.isActive());
		return ResponseEntity.ok(adminService.setActive(activeUserDTO.id(), activeUserDTO.isActive()));
	}

	/**
	 * {@code PUT /admin/users/block/:id} : block the user with the given id.
	 *
	 * @param login the login of the user to delete.
	 * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
	 */
	@PutMapping("/users/block/{id}")
	public ResponseEntity<User> blockUser(@PathVariable("id") Long id) {
		log.debug("REST request to block User : {}", id);
		return ResponseEntity.ok(adminService.updateBlockStatus(id, true));
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
}