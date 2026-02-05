package org.ngelmakproject.web.rest.errors;

public class InvalidPasswordException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public InvalidPasswordException() {
        super("The provided password does not match with existing.", "user", "invalidPassword");
    }
}
