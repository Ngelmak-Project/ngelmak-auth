package org.ngelmakproject.web.rest;

import java.util.Optional;

import org.ngelmakproject.domain.User;
import org.ngelmakproject.repository.UserRepository;
import org.ngelmakproject.service.MailService;
import org.ngelmakproject.service.UserService;
import org.ngelmakproject.web.rest.dto.PasswordChangeDTO;
import org.ngelmakproject.web.rest.dto.UserDTO;
import org.ngelmakproject.web.rest.errors.BadRequestAlertException;
import org.ngelmakproject.web.rest.errors.EmailAlreadyUsedException;
import org.ngelmakproject.web.rest.errors.InvalidPasswordException;
import org.ngelmakproject.web.rest.errors.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the current user's account.
 * /api
 * └── /user # User-level secured endpoints
 * │ ├── GET /profile
 * │ ├── PUT /update
 * │ └── POST /change-password
 */
@RestController
@RequestMapping("/api/user")
@PreAuthorize("isAuthenticated()")
public class UserResource {

    @ResponseStatus(HttpStatus.NOT_FOUND) // Or @ResponseStatus(HttpStatus.NO_CONTENT)
    private static class UserResourceException extends RuntimeException {
        private UserResourceException(String message) {
            super(message);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(UserResource.class);

    private static final String ENTITY_NAME = "user";

    @Value("${spring.application.name}")
    private String applicationName;

    private final UserRepository userRepository;
    private final UserService userService;
    private final MailService mailService;

    public UserResource(UserRepository userRepository, UserService userService, MailService mailService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.mailService = mailService;
    }

    /**
     * {@code GET  /profile} : get the current user.
     *
     * @return the current user.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user
     *                          couldn't be returned.
     */
    @GetMapping("/profile")
    public UserDTO getUser() {
        log.debug("Request to get current User details");
        return userService.getUserWithAuthorities().map(userPrincipal -> {
            return userRepository.findById(userPrincipal.id())
                    .map(UserDTO::from)
                    .orElseThrow(() -> new UserResourceException("User could not be found"));
        }).orElseThrow(UserNotFoundException::new);
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
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserDTO userDTO) {
        log.debug("REST request to update User : {}", userDTO);
        if (userDTO.id() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        var newAccount = userService.updateUser(userDTO);
        return ResponseEntity.ok()
                .body(UserDTO.from(newAccount));
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
        Optional<User> user = userService.activateRegistration(key);
        if (!user.isPresent()) {
            throw new UserResourceException("No user was found for this activation key");
        }
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
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
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
