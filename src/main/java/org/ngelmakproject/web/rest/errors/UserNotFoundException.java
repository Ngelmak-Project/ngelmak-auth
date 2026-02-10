package org.ngelmakproject.web.rest.errors;

/**
 * Exception thrown when a user is not found in the database.
 */
public class UserNotFoundException extends ResourceNotFoundException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException() {
        super("User not found.", "user", "userNotFound");
    }
}
