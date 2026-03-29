package org.ngelmakproject.web.rest.errors;

public class UserAlreadyActivatedException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public UserAlreadyActivatedException() {
        super("User is already activated !", "user", "userAlreadyActivated");
    }
}
