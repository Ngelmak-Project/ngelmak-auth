package org.ngelmakproject.web.rest.errors;

public class UserNotFoundException extends ResourceNotFoundException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException() {
        super("User not found.", "user", "userNotFound");
    }
}
