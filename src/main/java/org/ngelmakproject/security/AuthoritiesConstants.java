package org.ngelmakproject.security;

import java.util.Set;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String MODERATOR = "ROLE_MODERATOR";


    private AuthoritiesConstants() {
    }

    /**
     * Get the set of all defined authority names. This can be used, for example, to
     * validate authority names when users request new authorities.
     */
    public static Set<String> getAuthorities() {
        return Set.of(ADMIN, USER, MODERATOR);
    }
}