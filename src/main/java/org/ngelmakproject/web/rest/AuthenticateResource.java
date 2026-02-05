package org.ngelmakproject.web.rest;

import java.util.Optional;

import org.ngelmakproject.domain.User;
import org.ngelmakproject.service.AuthenticateService;
import org.ngelmakproject.service.UserService;
import org.ngelmakproject.web.rest.dto.LoginRequestDTO;
import org.ngelmakproject.web.rest.dto.RegisterRequestDTO;
import org.ngelmakproject.web.rest.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Controller to authenticate users.
 * /api
 * └── /public # Unsecured endpoints
 * │ └── /auth
 * │ │ ├── POST /authenticate
 * │ │ ├── POST /register
 * │ │ ├── POST /activate
 * │ │ ├── POST /password-reset
 * │ │ ├── /request
 * │ │ └── /complete
 */
@RestController
@RequestMapping("/api/public")
public class AuthenticateResource {

    private static final Logger log = LoggerFactory.getLogger(AuthenticateResource.class);

    private static final String ENTITY_NAME = "user";

    @Value("${spring.application.name}")
    private String applicationName;

    private final AuthenticateService authService;
    private final UserService userService;

    public AuthenticateResource(AuthenticateService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

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

    @PostMapping("/auth/register")
    public ResponseEntity<UserDTO> register(
            @RequestBody RegisterRequestDTO userDTO) {
        log.debug("REST request to register a new User : {}", userDTO);
        User newUser = userService.register(userDTO);
        return ResponseEntity.ok()
                .body(UserDTO.from(newUser));
    }

    @GetMapping("/auth/validate")
    public ResponseEntity<Void> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        // Authorization: Bearer <token>
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return authService.validateToken(authHeader.substring(7))
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

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