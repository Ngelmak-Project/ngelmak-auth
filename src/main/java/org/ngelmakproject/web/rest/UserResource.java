package org.ngelmakproject.web.rest;

import org.ngelmakproject.domain.User;
import org.ngelmakproject.service.MailService;
import org.ngelmakproject.service.UserService;
import org.ngelmakproject.web.rest.dto.PasswordChangeDTO;
import org.ngelmakproject.web.rest.dto.UserDTO;
import org.ngelmakproject.web.rest.dto.UserUpdateDTO;
import org.ngelmakproject.web.rest.errors.EmailAlreadyUsedException;
import org.ngelmakproject.web.rest.errors.InvalidPasswordException;
import org.ngelmakproject.web.rest.errors.LoginAlreadyUsedException;
import org.ngelmakproject.web.rest.errors.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the current user's account.
 * /api
 * └── /user # User-level secured endpoints
 * │ ├── GET /profile
 * │ ├── PUT /update
 * │ ├── PUT /upload-avatar
 * │ ├── POST /change-password
 * │ ├── POST /update-email
 * │ └── POST /certifications
 */
@RestController
@RequestMapping("/api/user")
@PreAuthorize("isAuthenticated()")
public class UserResource {

    private static final Logger log = LoggerFactory.getLogger(UserResource.class);

    private static final String ENTITY_NAME = "user";

    @Value("${spring.application.name}")
    private String applicationName;

    private final UserService userService;
    private final MailService mailService;

    public UserResource(UserService userService, MailService mailService) {
        this.userService = userService;
        this.mailService = mailService;
    }

    public record LoginUpdateDTO(String login) {
    }

    public record EmailUpdateDTO(String email) {
    }

    /**
     * {@code GET  /profile} : get the current user.
     *
     * @return the current user.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user
     *                          couldn't be returned.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUser() {
        log.debug("Request to get current User details");
        return ResponseEntity.ok(UserDTO.from(userService.profile()));
    }

    /**
     * {@code PUT  /update} : update the current user information.
     *
     * @param userDTO the current user information.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is
     *                                   already used.
     * @throws RuntimeException          {@code 500 (Internal Server Error)} if the
     *                                   user login wasn't found.
     */
    @PutMapping("/update")
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserUpdateDTO userUpdateDTO) {
        log.debug("REST request to update User : {}", userUpdateDTO);
        return ResponseEntity.ok()
                .body(UserDTO.from(userService.updateUser(userUpdateDTO)));
    }

    /**
     * {@code GET  /users/activate} : activate the registered user.
     *
     * @param key the activation key.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user
     *                          couldn't be activated.
     */
    @GetMapping("/activate")
    public void activateUser(@RequestParam(value = "key") String key) {
        log.debug("REST request to activate User's email");
        userService.activateRegistration(key);
    }

    /**
     * {@code POST  /change-password} : changes the current user's password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new
     *                                  password is incorrect.
     */
    @PostMapping("/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        log.debug("REST request to change User's password : {}", passwordChangeDto);
        userService.changePassword(passwordChangeDto.currentPassword(), passwordChangeDto.newPassword());
    }

    /**
     * REST endpoint to update the current user's email address.
     *
     * @param emailUpdateDTO DTO containing the new email address
     * @return ResponseEntity with the updated UserDTO
     * @throws EmailAlreadyUsedException If the email is already registered by
     *                                   another user
     * @throws UserNotFoundException     If no user is found for the current
     *                                   authentication context
     */
    @PostMapping("/update-email")
    public ResponseEntity<UserDTO> updateEmail(@RequestBody EmailUpdateDTO emailUpdateDTO) {
        log.debug("REST request to update User's email : {}", emailUpdateDTO.email());
        User user = userService.updateEmail(emailUpdateDTO.email());
        return ResponseEntity.ok(UserDTO.from(user));
    }

    /**
     * REST endpoint to update the current user's login.
     *
     * @param loginUpdateDTO DTO containing the new login
     * @return ResponseEntity with the updated UserDTO
     * @throws LoginAlreadyUsedException If the login is already taken by another
     *                                   user
     * @throws UserNotFoundException     If no user is found for the current
     *                                   authentication context
     */
    @PostMapping("/update-login")
    public ResponseEntity<UserDTO> updateLogin(@RequestBody LoginUpdateDTO loginUpdateDTO) {
        log.debug("REST request to update User's login : {}", loginUpdateDTO.login());
        User user = userService.updateLogin(loginUpdateDTO.login());
        return ResponseEntity.ok(UserDTO.from(user));
    }

    /**
     * {@code POST   /users/reset-password/finish} : Finish to reset the password
     * of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is
     *                                  incorrect.
     * @throws RuntimeException         {@code 500 (Internal Server Error)} if the
     *                                  password could not be reset.
     */
    // @PostMapping("/reset-password/finish")
    // public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword)
    // {
    // if (isPasswordLengthInvalid(keyAndPassword.getNewPassword())) {
    // throw new InvalidPasswordException();
    // }
    // Optional<User> user =
    // userService.completePasswordReset(keyAndPassword.getNewPassword(),
    // keyAndPassword.getKey());

    // if (!user.isPresent()) {
    // throw new UserResourceException("No user was found for this reset key");
    // }
    // }

    /**
     * {@code PUT   /users/upload-image} : Upload an image for the current user.
     * 
     * @param file
     * @return the current user.
     */
    // @PutMapping("/upload-image")
    // @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.USER + "\")")
    // public ResponseEntity<UserDTO> upload(@RequestParam("file") MultipartFile
    // file) {
    // log.debug("REST request to upload the user's account image");
    // userService.upload(file);
    // return ResponseUtil.wrapOrNotFound(userService.upload(file));
    // }
}
