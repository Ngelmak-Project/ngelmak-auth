package org.ngelmakproject.web.rest.errors;

public class UserNotActivatedException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public UserNotActivatedException() {
        super("User is not activated !", "user", "userNotActivated");
    }
}
