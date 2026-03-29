package org.ngelmakproject.web.rest;

import java.util.Optional;

import org.ngelmakproject.domain.User;
import org.ngelmakproject.service.AuthenticateService;
import org.ngelmakproject.web.rest.dto.LoginRequestDTO;
import org.ngelmakproject.web.rest.dto.RegisterRequestDTO;
import org.ngelmakproject.web.rest.dto.UserDTO;
import org.ngelmakproject.web.rest.errors.UserAlreadyActivatedException;
import org.ngelmakproject.web.rest.errors.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Public Authentication Controller.
 * 
 * <p>
 * Base path: /api/public/
 * 
 * - /api
 * - └── /public # Unsecured endpoints
 * - │ ├── /auth # Authentication & account lifecycle
 * - │ │ ├── POST /authenticate
 * - │ │ ├── POST /register
 * - │ │ ├── POST /register
 * - │ │ ├── GET /activate?key=...
 * - │ │ ├── POST /activate/resend
 * - │ │ ├── POST /password-reset/init
 * - │ │ └── POST /password-reset/finish
 * - │ │
 * - │ └── /support # Public contact/support forms
 * - │ │ └── POST /contact
 */
@RestController
@RequestMapping("/api/public")
public class AuthenticateResource {

    private static final Logger log = LoggerFactory.getLogger(AuthenticateResource.class);

    @Value("${spring.application.name}")
    private String applicationName;

    private final AuthenticateService authService;

    public AuthenticateResource(AuthenticateService authService) {
        this.authService = authService;
    }

    /**
     * {@code POST  /auth/authenticate} : Authenticates a user using login
     * credentials and returns a JWT token.
     *
     * <ul>
     * <li>Validates login credentials</li>
     * <li>Ensures the account is activated and not blocked</li>
     * <li>Generates a JWT token (standard or remember-me)</li>
     * </ul>
     *
     * @param loginRequestDTO DTO containing login, password, and remember-me flag.
     * @return {@code 200 (OK)} with JWT token if authentication succeeds,
     *         401 Unauthorized if credentials are invalid
     */
    @PostMapping("/auth/authenticate")
    public ResponseEntity<JWTToken> authenticate(
            @RequestBody LoginRequestDTO loginRequestDTO) {
        log.debug("REST request for loging User : {}", loginRequestDTO);
        Optional<String> tokenOptional = authService.authenticate(loginRequestDTO);

        if (tokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = tokenOptional.get();
        return ResponseEntity.ok(new JWTToken(token));
    }

    /**
     * {@code POST  /auth/activate?key=...} : Authenticates a user using login
     * credentials and returns a JWT token.
     *
     * <ul>
     * <li>Validates login credentials</li>
     * <li>Ensures the account is activated and not blocked</li>
     * <li>Generates a JWT token (standard or remember-me)</li>
     * </ul>
     *
     * @param loginRequestDTO DTO containing login, password, and remember-me flag.
     * @return {@code 200 (OK)} with the created UserDTO
     */
    @PostMapping("/auth/register")
    public ResponseEntity<UserDTO> register(
            @RequestBody RegisterRequestDTO userDTO) {
        log.debug("REST request to register a new User : {}", userDTO);
        User newUser = authService.register(userDTO);
        return ResponseEntity.ok()
                .body(UserDTO.from(newUser));
    }

    /**
     * {@code GET  /auth/activate?key=...} : Activates a user account using a
     * temporary
     * verification key.
     *
     * @param key the activation key.
     * @throws UserNotFoundException {@code 500 (Internal Server Error)} if the user
     *                               couldn't be activated.
     */
    @GetMapping("/auth/activate")
    public void activateUser(@RequestParam String key) {
        log.debug("REST request to activate User's email");
        authService.activateUserByKey(key);
    }

    private record EmailResetDTO(String email) {
    }

    /**
     * {@code POST  /auth/activate/resend} : Resends the activation email to the
     * user.
     *
     * @param emailResetDTO the user's email address.
     * @throws UserAlreadyActivatedException {@code 400 (Bad Request)} If the user
     *                                       is already activated.
     */
    @PostMapping("/auth/activate/resend")
    public void resendActivation(@RequestBody EmailResetDTO emailResetDTO) {
        log.debug("REST request to resend activation email for {}", emailResetDTO.email);
        authService.resendActivation(emailResetDTO.email);
    }

    /**
     * {@code POST  /auth/reset-password/init} : Initiates a password reset request
     * by
     * generating a temporary key and sending it to the user's email address.
     *
     * @param email the user's email address
     */
    @PostMapping("/auth/reset-password/init")
    public void requestPasswordReset(@RequestBody EmailResetDTO resetDTO) {
        log.debug("REST request to initiate password reset for {}", resetDTO.email);
        authService.requestPasswordReset(resetDTO.email);
    }

    private record CompletePasswordResetDTO(String key, String newPassword) {
    }

    /**
     * {@code POST  /auth/reset-password/finish} : Completes the password reset
     * process
     * by validating the temporary key and setting a new password.
     *
     * @param key         the temporary reset key
     * @param newPassword the new password chosen by the user
     */
    @PostMapping("/auth/reset-password/finish")
    public void completePasswordReset(@RequestBody CompletePasswordResetDTO resetDTO) {
        log.debug("REST request to complete password reset : {}", resetDTO);
        authService.completePasswordReset(resetDTO.key, resetDTO.newPassword);
    }

    // SUPPORT

    private record ContactRequestDTO(String name, String email, String subject, String message) {
    }

    /**
     * {@code POST  /support/contact} : Endpoint to send a contact message to
     * support.
     *
     * @param contactRequestDTO the contact message information.
     * @return {@code 200 (OK)} if the message was sent successfully.
     */
    @PostMapping("/support/contact")
    public ResponseEntity<Void> contact(@RequestBody ContactRequestDTO contactRequestDTO) {
        log.debug("REST request to send a contact message : {}", contactRequestDTO);
        authService.contactSupport(contactRequestDTO.name(), contactRequestDTO.email(),
                contactRequestDTO.subject(), contactRequestDTO.message());
        return ResponseEntity.ok().build();
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {
        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }

}