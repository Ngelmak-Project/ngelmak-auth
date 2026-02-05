package org.ngelmakproject.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.ngelmakproject.domain.Authority;
import org.ngelmakproject.domain.User;

public record UserPrincipal(
        Long id,
        String login,
        String firstName,
        String lastName,
        String email,
        Set<String> authorities) {

    public static UserPrincipal from(User u) {
        return new UserPrincipal(
                u.getId(),
                u.getLogin(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getAuthorities().stream()
                        .map(Authority::getName)
                        .collect(Collectors.toSet()));
    }
}
