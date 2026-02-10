package org.ngelmakproject.web.rest.errors;

public class UserBlockedException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public UserBlockedException() {
        super("User is blocked!", "user", "userBlocked");
    }
}
