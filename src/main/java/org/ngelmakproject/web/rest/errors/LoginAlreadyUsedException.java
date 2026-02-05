package org.ngelmakproject.web.rest.errors;

public class LoginAlreadyUsedException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public LoginAlreadyUsedException() {
        super("Login already used.", "user", "loginExists");
    }
}
