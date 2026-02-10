package org.ngelmakproject.web.rest.dto;

import jakarta.validation.constraints.NotNull;

/**
 * A DTO representing a change in a user's active status.
 */
public record ActiveUserDTO(
		@NotNull(message = "ID is required") Long id,
		@NotNull(message = "Active status is required") Boolean isActive) {
}