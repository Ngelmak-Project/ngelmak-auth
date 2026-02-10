package org.ngelmakproject.web.rest.dto;

/**
 * A DTO representing a user.
 */
public record UserUpdateDTO(
    String firstName,
    String lastName,
    String langKey,
    Boolean darkModeEnabled) {
}
