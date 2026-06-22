package org.ngelmakproject.web.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * View Model object for storing a user's info.
 */
public record RegisterRequestDTO(
		@NotNull(message = "Email cannot be null") @Size(min = 4, max = 50, message = "Email must be between 4 and 50 characters") String email,

		@NotNull(message = "Login cannot be null") @Size(min = 4, max = 50, message = "Login must be between 4 and 50 characters") String login,

		@NotNull(message = "Password cannot be null") @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters") String password,

		String langKey) {
}