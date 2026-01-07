package org.ngelmakproject.security;

import java.util.Set;

/**
 * Internal representation of the authenticated user.
 */
public class UserPrincipal {

    private final Long id;
    private final String username;
    private final String firstname;
    private final String lastname;
    private final String email;
    private final Set<String> authorities;

    public UserPrincipal(Long id, String username, String firstname, String lastname, String email,
            Set<String> authorities) {
        this.id = id;
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.authorities = authorities;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    @Override
    public String toString() {
        return "UserPrincipal [id=" + id + ", username=" + username + ", firstname=" + firstname + ", lastname="
                + lastname + ", email=" + email + ", authorities=" + authorities + "]";
    }

}
