package org.ngelmakproject.web.rest;

import java.time.Instant;
import java.util.Optional;

import org.ngelmakproject.domain.User;
import org.ngelmakproject.service.AuthenticateService;
import org.ngelmakproject.service.email.MailService;
import org.ngelmakproject.web.rest.dto.AuthenticationResponse;
import org.ngelmakproject.web.rest.dto.LoginRequestDTO;
import org.ngelmakproject.web.rest.dto.RegisterRequestDTO;
import org.ngelmakproject.web.rest.dto.UserDTO;
import org.ngelmakproject.web.rest.errors.UserAlreadyActivatedException;
import org.ngelmakproject.web.rest.errors.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication API
 *
 * <p>
 * Base path: /api/v1
 *
 * <p>
 * Public endpoints for user authentication, registration, account activation,
 * password management, and support contact.
 *
 * <h3>Endpoints</h3>
 * <ul>
 * <li>{@code POST /login} - Authenticate user and get JWT token</li>
 * <li>{@code POST /register} - Create new user account</li>
 * <li>{@code GET /activate} - Activate account via email key</li>
 * <li>{@code POST /activate/resend} - Resend activation email</li>
 * <li>{@code POST /password-reset/init} - Request password reset</li>
 * <li>{@code POST /password-reset/finish} - Complete password reset</li>
 * <li>{@code POST /contact} - Send support contact message</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
public class AuthenticateResource {

    private static final Logger log = LoggerFactory.getLogger(AuthenticateResource.class);

    @Value("${spring.application.name}")
    private String applicationName;

    private final AuthenticateService authService;
    private final MailService mailService;

    public AuthenticateResource(AuthenticateService authService,
            MailService mailService) {
        this.authService = authService;
        this.mailService = mailService;
    }

    /**
     * {@code POST  /login} : Authenticates a user using login
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
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> authenticate(
            @RequestBody LoginRequestDTO request) {
        log.debug("REST request for loging User : {}", request);
        Optional<AuthenticationResponse> optional = authService.authenticate(request);

        if (optional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var auth = optional.get();

        ResponseCookie cookie = ResponseCookie.from("refreshToken", auth.rt().getToken())
                .httpOnly(true)
                // .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(auth.rt().getExpiresAt().getEpochSecond())
                .build();

        // Body for mobile apps
        LoginResponseDTO body = new LoginResponseDTO(
                auth.accessToken(),
                auth.rt().getToken(),
                auth.rt().getExpiresAt());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    /**
     * {@code POST /refresh} : Issues a new access token using a valid refresh
     * token.
     *
     * Accepts the refresh token from either the cookie (browser clients)
     * or the request body (mobile clients). Returns a new access token and,
     * for mobile clients, a new refresh token.
     *
     * @param cookieToken the refresh token from the cookie (browser), may be null
     * @param body        optional DTO containing the refresh token (mobile), may be
     *                    null
     * @return {@code 200 (OK)} with new tokens if refresh succeeds,
     *         {@code 401 (Unauthorized)} if the refresh token is missing or invalid
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(
            @CookieValue(value = "refreshToken", required = false) String cookieToken,
            @RequestBody(required = false) RefreshRequestDTO body) {

        log.info("REST request to refresh token with cookieToken: {} and body: {}", cookieToken, body);

        String token = cookieToken != null ? cookieToken : (body != null ? body.refreshToken() : null);

        if (token == null) {
            log.warn("No refresh token provided in cookie or request body");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<AuthenticationResponse> optional = authService.refreshToken(token);

        if (optional.isEmpty()) {
            log.warn("Invalid refresh token provided");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var auth = optional.get();

        ResponseCookie cookie = ResponseCookie.from("refreshToken", auth.rt().getToken())
                .httpOnly(true)
                // .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(auth.rt().getExpiresAt().getEpochSecond())
                .build();

        LoginResponseDTO bodyResponse = new LoginResponseDTO(
                auth.accessToken(),
                auth.rt().getToken(),
                auth.rt().getExpiresAt());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(bodyResponse);
    }

    /**
     * {@code POST  /register} : Registers a new user account and sends an
     * activation email.
     *
     * @param userDTO the user registration information.
     * @throws UserAlreadyActivatedException {@code 400 (Bad Request)} If the user
     *                                       is already activated.
     * @apiNote After successful registration, an activation email is sent to the
     * @return {@code 200 (OK)} with the created UserDTO
     */
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(
            @RequestBody RegisterRequestDTO userDTO) {
        log.debug("REST request to register a new User : {}", userDTO);
        User newUser = authService.register(userDTO);
        mailService.sendActivationEmail(newUser);
        return ResponseEntity.ok()
                .body(UserDTO.from(newUser));
    }

    /**
     * {@code GET  /activate?key=...} : Activates a user account using a
     * temporary
     * verification key.
     *
     * @param key the activation key.
     * @throws UserNotFoundException {@code 500 (Internal Server Error)} if the user
     *                               couldn't be activated.
     * @apiNote After successful activation, a welcome email is sent to the user.
     */
    @GetMapping("/activate")
    public void activateUser(@RequestParam String key) {
        log.debug("REST request to activate User's email");
        User user = authService.activateUserByKey(key);
        mailService.sendWelcomeEmail(user);
    }

    private record EmailResetDTO(String email) {
    }

    /**
     * {@code POST  /activate/resend} : Resends the activation email to the
     * user.
     *
     * @param emailResetDTO the user's email address.
     * @throws UserAlreadyActivatedException {@code 400 (Bad Request)} If the user
     *                                       is already activated.
     * @apiNote If the email is associated with an existing account that is not yet
     *          activated, a new activation email will be sent.
     */
    @PostMapping("/activate/resend")
    public void resendActivation(@RequestBody EmailResetDTO emailResetDTO) {
        log.debug("REST request to resend activation email for {}", emailResetDTO.email);
        authService.resendActivation(emailResetDTO.email)
                .ifPresent(mailService::sendActivationEmail);
    }

    /**
     * {@code POST  /password-reset/init} : Initiates a password reset request
     * by
     * generating a temporary key and sending it to the user's email address.
     *
     * @param email the user's email address
     * @apiNote If the email is associated with an existing account, a password
     *          reset email will be sent containing a temporary key for resetting
     *          the password.
     */
    @PostMapping("/password-reset/init")
    public void requestPasswordReset(@RequestBody EmailResetDTO resetDTO) {
        log.debug("REST request to initiate password reset for {}", resetDTO.email);
        authService.requestPasswordReset(resetDTO.email)
                .ifPresent(mailService::sendPasswordResetEmail);
    }

    private record CompletePasswordResetDTO(String key, String newPassword) {
    }

    /**
     * {@code POST  /password-reset/finish} : Completes the password reset
     * process
     * by validating the temporary key and setting a new password.
     *
     * @param key         the temporary reset key
     * @param newPassword the new password chosen by the user
     */
    @PostMapping("/password-reset/finish")
    public void completePasswordReset(@RequestBody CompletePasswordResetDTO resetDTO) {
        log.debug("REST request to complete password reset : {}", resetDTO);
        authService.completePasswordReset(resetDTO.key, resetDTO.newPassword);
    }

    // SUPPORT

    private record ContactRequestDTO(String name, String email, String subject, String message) {
    }

    /**
     * {@code POST  /contact} : Endpoint to send a contact message to
     * support.
     *
     * @param contactRequestDTO the contact message information.
     * @return {@code 200 (OK)} if the message was sent successfully.
     */
    @PostMapping("/contact")
    public ResponseEntity<Void> contact(@RequestBody ContactRequestDTO contactRequestDTO) {
        log.debug("REST request to send a contact message : {}", contactRequestDTO);
        authService.contactSupport(contactRequestDTO.name(), contactRequestDTO.email(),
                contactRequestDTO.subject(), contactRequestDTO.message());
        return ResponseEntity.ok().build();
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    public record LoginResponseDTO(
            String accessToken,
            String refreshToken,
            Instant refreshTokenExpiresAt) {
    }

    public record RefreshRequestDTO(String refreshToken) {
    }

}