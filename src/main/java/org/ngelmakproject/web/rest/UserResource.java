package org.ngelmakproject.web.rest;

import java.util.Optional;

import org.ngelmakproject.domain.User;
import org.ngelmakproject.repository.UserRepository;
import org.ngelmakproject.service.MailService;
import org.ngelmakproject.service.UserService;
import org.ngelmakproject.web.rest.dto.PasswordChangeDTO;
import org.ngelmakproject.web.rest.dto.RegisterRequestDTO;
import org.ngelmakproject.web.rest.dto.UserDTO;
import org.ngelmakproject.web.rest.errors.EmailAlreadyUsedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api/me")
public class UserResource {

    @ResponseStatus(HttpStatus.NOT_FOUND) // Or @ResponseStatus(HttpStatus.NO_CONTENT)
    private static class AccountResourceException extends RuntimeException {
        private AccountResourceException(String message) {
            super(message);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(UserResource.class);

    private final UserRepository userRepository;

    private final UserService userService;

    private final MailService mailService;

    public UserResource(UserRepository userRepository, UserService userService, MailService mailService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.mailService = mailService;
    }

    /**
     * {@code GET  /users} : get the current user.
     *
     * @return the current user.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user
     *                          couldn't be returned.
     */
    @GetMapping("")
    public UserDTO getAccount() {
        var user = userService.getUserWithAuthorities();
        return userRepository.findById(user.getId())
                .map(UserDTO::new)
                .orElseThrow(() -> new AccountResourceException("User could not be found"));
    }

    /**
     * {@code POST  /users} : update the current user information.
     *
     * @param userDTO the current user information.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is
     *                                   already used.
     * @throws RuntimeException          {@code 500 (Internal Server Error)} if the
     *                                   user login wasn't found.
     */
    // @PostMapping("")
    // public void saveAccount(@Valid @RequestBody UserDTO userDTO) {
    // String userLogin = SecurityUtils.getCurrentUserLogin()
    // .orElseThrow(() -> new AccountResourceException("Current user login not
    // found"));
    // Optional<User> existingUser =
    // userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
    // if (existingUser.isPresent() &&
    // (!existingUser.orElseThrow().getLogin().equalsIgnoreCase(userLogin))) {
    // throw new EmailAlreadyUsedException();
    // }
    // Optional<User> user = userRepository.findOneByLogin(userLogin);
    // if (!user.isPresent()) {
    // throw new AccountResourceException("User could not be found");
    // }
    // userService.updateUser(
    // userDTO.getFirstName(),
    // userDTO.getLastName(),
    // userDTO.getEmail(),
    // userDTO.getLangKey());
    // }

    /**
     * {@code POST  /register} : register the user.
     *
     * @param managedUserVM the managed user View Model.
     * @throws InvalidPasswordException  {@code 400 (Bad Request)} if the password
     *                                   is incorrect.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is
     *                                   already used.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is
     *                                   already used.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAccount(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        log.debug("About to register a new user {}", registerRequestDTO);
        User user = userService.registerUser(registerRequestDTO, registerRequestDTO.getPassword());
        log.debug("User {} has been created with success!", user.getLogin());
    }

    /**
     * {@code GET  /users/activate} : activate the registered user.
     *
     * @param key the activation key.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user
     *                          couldn't be activated.
     */
    @GetMapping("/activate")
    public void activateAccount(@RequestParam(value = "key") String key) {
        Optional<User> user = userService.activateRegistration(key);
        if (!user.isPresent()) {
            throw new AccountResourceException("No user was found for this activation key");
        }
    }

    /**
     * {@code PUT /users/certifications/request} : requestion
     * certification for a user account.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         all users.
     */
    // @PutMapping("/certifications/request")
    // @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.USER + "\")")
    // public ResponseEntity<UserDTO> certificationRequest(
    // @Valid @RequestBody AccountCertificationRequestDTO requestDTO) {
    // log.debug("REST request to certify the connected User account");
    // return
    // ResponseUtil.wrapOrNotFound(userService.certificationRequest(requestDTO));
    // }

    /**
     * {@code POST  /users/change-password} : changes the current user's password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new
     *                                  password is incorrect.
     */
    @PostMapping("/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    /**
     * {@code POST   /users/reset-password/init} : Send an email to reset the
     * password of the user.
     *
     * @param mail the mail of the user.
     */
    @PostMapping("/reset-password/init")
    public void requestPasswordReset(@RequestBody String mail) {
        Optional<User> user = userService.requestPasswordReset(mail);
        if (user.isPresent()) {
            mailService.sendPasswordResetMail(user.orElseThrow());
        } else {
            // Pretend the request has been successful to prevent checking which emails
            // really exist
            // but log that an invalid attempt has been made
            log.warn("Password reset requested for non existing mail");
        }
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
    // public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
    //     if (isPasswordLengthInvalid(keyAndPassword.getNewPassword())) {
    //         throw new InvalidPasswordException();
    //     }
    //     Optional<User> user = userService.completePasswordReset(keyAndPassword.getNewPassword(),
    //             keyAndPassword.getKey());

    //     if (!user.isPresent()) {
    //         throw new AccountResourceException("No user was found for this reset key");
    //     }
    // }

    /**
     * {@code PUT   /users/upload-image} : Upload an image for the current user.
     * 
     * @param file
     * @return the current user.
     */
    // @PutMapping("/upload-image")
    // @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.USER + "\")")
    // public ResponseEntity<UserDTO> upload(@RequestParam("file") MultipartFile file) {
    //     log.debug("REST request to upload the user's account image");
    //     userService.upload(file);
    //     return ResponseUtil.wrapOrNotFound(userService.upload(file));
    // }
}
