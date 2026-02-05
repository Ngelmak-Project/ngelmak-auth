package org.ngelmakproject.web.rest;

import java.util.concurrent.TimeUnit;

import org.ngelmakproject.repository.UserRepository;
import org.ngelmakproject.security.AuthoritiesConstants;
import org.ngelmakproject.service.MailService;
import org.ngelmakproject.service.UserService;
import org.ngelmakproject.web.rest.dto.PageDTO;
import org.ngelmakproject.web.rest.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	@Value("${ngelmak.clientApp.name}")
	private String applicationName;

	private final UserService userService;
	private final UserRepository userRepository;
	private final MailService mailService;

	public AdminResource(UserService userService, UserRepository userRepository, MailService mailService) {
		this.userService = userService;
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
	public ResponseEntity<PageDTO<UserDTO>> getAllUsers(Pageable pageable) {
		log.debug("REST request to get all User for an admin");
		return ResponseEntity.ok().cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
				.body(new PageDTO<>(userService.getAllManagedUsers(pageable)));
	}

	/**
	 * {@code GET /admin/users/:login} : get the "login" user.
	 *
	 * @param login the login of the user to find.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the "login" user, or with status {@code 404 (Not Found)}.
	 */
	// @GetMapping("/users/{login}")
	// @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
	// public ResponseEntity<UserDTO> getUser(
	// @PathVariable("login") @Pattern(regexp = Constants.LOGIN_REGEX) String login)
	// {
	// log.debug("REST request to get User : {}", login);
	// return
	// ResponseUtil.wrapOrNotFound(userService.getUserWithAuthoritiesByLogin(login).map(UserDTO::new));
	// }

	/**
	 * {@code DELETE /admin/users/:id} : delete the "id" User.
	 *
	 * @param id the id of the user to delete.
	 * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
	 */
	@DeleteMapping("/users/{id}")
	public ResponseEntity<Void> deleteUser(
			@PathVariable("id") Long id) {
		log.debug("REST request to delete User: {}", id);
		// userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * {@code Put /admin/users/certification} : requestion
	 * certification for a user account.
	 *
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the account just certified.
	 */
	// @PutMapping("/users/certification")
	// @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
	// public ResponseEntity<UserDTO> certification(
	// @Valid @RequestBody AccountCertificationRequestDTO requestDTO) {
	// log.debug("REST request to certify User : {}", requestDTO);
	// return ResponseUtil.wrapOrNotFound(userService.certificate(requestDTO));
	// }

	/**
	 * {@code Put /admin/users/certification} : requestion
	 * certification for a user account.
	 *
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the account just certified.
	 */
	// @PutMapping("/users/certification-withdrawal/{login}")
	// @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
	// public ResponseEntity<UserDTO> certificationWithdrawal(
	// @PathVariable("login") String login) {
	// log.debug("REST request to withdraw certification User : {}", login);
	// return
	// ResponseUtil.wrapOrNotFound(userService.certificationWithdrawal(login));
	// }

	/**
	 * {@code Put /admin/users/certification/:login} : requestion
	 * certification for a user account.
	 *
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         user certification details.
	 */
	// @GetMapping("/users/certification/{login}")
	// @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
	// public ResponseEntity<AccountCertificationRequestDTO>
	// getAccountCertification(
	// @PathVariable("login") String login) {
	// log.debug("REST request to get account certification of User : {}", login);
	// return
	// ResponseUtil.wrapOrNotFound(userService.getAccountCertification(login));
	// }

}