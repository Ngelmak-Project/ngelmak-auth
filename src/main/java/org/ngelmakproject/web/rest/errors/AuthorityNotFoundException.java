package org.ngelmakproject.web.rest.errors;

/**
 * Exception thrown when an authority is not found.
 */
public class AuthorityNotFoundException extends ResourceNotFoundException {

    private static final long serialVersionUID = 1L;

    public AuthorityNotFoundException(String authorityName) {
        super("Authority not found: " + authorityName, "authority", "authorityNotFound");
    }
}