package org.ngelmakproject.web.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * View Model object for storing a user's credentials.
 */
public record LoginRequestDTO(
        @NotNull(message = "Login is requed") @Size(min = 3, max = 50) String login,
        @NotNull(message = "Password is requed") @Size(min = 8, max = 100) String password,
        boolean rememberMe) {
}