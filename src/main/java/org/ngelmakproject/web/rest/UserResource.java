package org.ngelmakproject.web.rest;

import java.util.List;

import org.ngelmakproject.domain.AuthorityRequest;
import org.ngelmakproject.domain.User;
import org.ngelmakproject.service.UserService;
import org.ngelmakproject.service.email.MailService;
import org.ngelmakproject.web.rest.dto.AuthorityRequestDTO;
import org.ngelmakproject.web.rest.dto.PasswordChangeDTO;
import org.ngelmakproject.web.rest.dto.UserDTO;
import org.ngelmakproject.web.rest.dto.UserUpdateDTO;
import org.ngelmakproject.web.rest.errors.AuthorityNotFoundException;
import org.ngelmakproject.web.rest.errors.EmailAlreadyUsedException;
import org.ngelmakproject.web.rest.errors.InvalidPasswordException;
import org.ngelmakproject.web.rest.errors.LoginAlreadyUsedException;
import org.ngelmakproject.web.rest.errors.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the current authenticated user's account.
 *
 * <p>
 * Base path: /api/v1/me
 *
 * <p>
 * Endpoints for the authenticated user to manage their profile, credentials,
 * authorities, and certifications.
 *
 * <p>
 * Requires: Authentication
 *
 * <h3>Profile Management</h3>
 * <ul>
 * <li>{@code GET /} - Get current user profile</li>
 * <li>{@code PUT /} - Update current user profile</li>
 * <li>{@code DELETE /} - Delete current user profile</li>
 * <li>{@code PUT /avatar} - Update user avatar</li>
 * </ul>
 *
 * <h3>Credentials & Email</h3>
 * <ul>
 * <li>{@code POST /password} - Change password</li>
 * <li>{@code PUT /email} - Update email address</li>
 * <li>{@code PUT /login} - Update login username</li>
 * </ul>
 *
 * <h3>Authorities & Certification</h3>
 * <ul>
 * <li>{@code GET /authorities} - Get authority requests</li>
 * <li>{@code POST /authorities} - Request new authority</li>
 * <li>{@code POST /certifications} - Create certification</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("isAuthenticated()")
public class UserResource {

    private static final Logger log = LoggerFactory.getLogger(UserResource.class);

    @Value("${spring.application.name}")
    private String applicationName;

    private final UserService userService;
    private final MailService mailService;

    public UserResource(UserService userService, MailService mailService) {
        this.userService = userService;
        this.mailService = mailService;
    }

    // DTO for authority request
    private record AccessApprovalDTO(String authorityName, String motivation) {
    }

    public record LoginUpdateDTO(String login) {
    }

    public record EmailUpdateDTO(String email) {
    }

    /**
     * {@code GET  /} : get the current user.
     *
     * @return the current user.
     * @throws UserNotFoundException {@code 404 (Resource Not Found)} If the user
     *                               couldn't be returned.
     */
    @GetMapping()
    public ResponseEntity<UserDTO> getUser() {
        log.debug("Request to get current User details");
        return ResponseEntity.ok(UserDTO.from(userService.profile()));
    }

    /**
     * {@code PUT  /} : update the current user information.
     *
     * @param userDTO the current user information.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} If the email is
     *                                   already used.
     * @throws UserNotFoundException     {@code 404 (Resource Not Found)} If the
     *                                   user login wasn't found.
     */
    @PutMapping("")
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserUpdateDTO userUpdateDTO) {
        log.debug("REST request to update User : {}", userUpdateDTO);
        return ResponseEntity.ok()
                .body(UserDTO.from(userService.updateUser(userUpdateDTO)));
    }

    /**
     * {@code DELETE  /} : delete the current user.
     *
     * @throws UserNotFoundException {@code 404 (Resource Not Found)} If the user
     *                               login wasn't found.
     */
    @DeleteMapping()
    public ResponseEntity<Void> deleteUser() {
        log.debug("REST request to delete current User");
        userService.deleteUser();
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code POST  /password} : changes the current user's password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} If the new
     *                                  password is incorrect.
     */
    @PostMapping("/password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        log.debug("REST request to change User's password : {}", passwordChangeDto);
        userService.changePassword(passwordChangeDto.currentPassword(), passwordChangeDto.newPassword());
    }

    /**
     * {@code PUT  /email} : REST endpoint to update the current user's email
     * address.
     *
     * @param emailUpdateDTO DTO containing the new email address
     * @return ResponseEntity with the updated UserDTO
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} If the email is
     *                                   already registered by another user
     * @throws UserNotFoundException     {@code 404 (Resource Not Found)} If no user
     *                                   is found for the current authentication
     *                                   context
     * @apiNote An email verification message is sent to the new email address with
     *          a link to confirm the change via
     *          {@link MailService#sendEmailVerificationEmail(User)}
     */
    @PutMapping("/email")
    public ResponseEntity<UserDTO> updateEmail(@RequestBody EmailUpdateDTO emailUpdateDTO) {
        log.debug("REST request to update User's email : {}", emailUpdateDTO.email());
        User user = userService.updateEmail(emailUpdateDTO.email());
        mailService.sendEmailVerificationEmail(user);
        return ResponseEntity.ok(UserDTO.from(user));
    }

    /**
     * {@code PUT /login} : Updates the current user's login.
     *
     * @param loginUpdateDTO DTO containing the new login
     * @return ResponseEntity with the updated UserDTO
     * @throws LoginAlreadyUsedException If the login is already taken
     * @throws UserNotFoundException     {@code 404 (Resource Not Found)} If no user
     *                                   is found for the current authentication
     *                                   context
     */
    @PutMapping("/login")
    public ResponseEntity<UserDTO> updateLogin(@RequestBody LoginUpdateDTO loginUpdateDTO) {
        log.debug("REST request to update User's login : {}", loginUpdateDTO.login());
        User user = userService.updateLogin(loginUpdateDTO.login());
        return ResponseEntity.ok(UserDTO.from(user));
    }

    /**
     * {@code POST /authorities} : Endpoint to request a new authority for
     * the current user.
     *
     * @param authorityName the name of the authority to request
     * @param motivation    the motivation for requesting the authority
     * @return the created {@link AuthorityRequest}
     * @throws AuthorityNotFoundException {@code 404 (Resource Not Found)} if the
     *                                    authority could not be found or the
     *                                    request could not be created
     * 
     * @apiNote An acknowledgment email is sent to the user confirming receipt of
     *          their authority (moderator) request via
     *          {@link MailService#sendModeratorRequestAcknowledgmentEmail(User)}
     */
    @PostMapping("/authorities")
    public AuthorityRequest requestAuthority(@RequestBody AccessApprovalDTO authorityRequestDTO) {
        log.debug("REST request to request authority {} with motivation {}", authorityRequestDTO.authorityName(),
                authorityRequestDTO.motivation());
        AuthorityRequest authorityRequest = userService.requestAuthority(authorityRequestDTO.authorityName(),
                authorityRequestDTO.motivation());
        mailService.sendModeratorRequestAcknowledgmentEmail(authorityRequest.getUser());
        return authorityRequest;
    }

    /**
     * {@code GET /authorities} : Endpoint to get the current user's
     * authority requests.
     *
     * @return a list of AuthorityRequestDTO representing the current user's
     *         authority requests
     */
    @GetMapping("/authorities")
    public ResponseEntity<List<AuthorityRequestDTO>> getAuthorityRequests() {
        log.debug("REST request to get current User's authority requests");
        return ResponseEntity.ok(userService.getCurrentUserAuthorityRequests());
    }
}
