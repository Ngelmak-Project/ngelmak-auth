package org.ngelmakproject.web.rest.dto;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import org.ngelmakproject.domain.Authority;
import org.ngelmakproject.domain.User;
import org.ngelmakproject.domain.enumeration.CertificationStatus;
import org.ngelmakproject.security.UserPrincipal;

/**
 * A DTO representing a user, with his authorities.
 */
public record UserDTO(
        Long id,
        String login,
        String firstName,
        String lastName,
        String email,
        boolean isActivated,
        String langKey,
        Instant createdDate,
        CertificationStatus certificationStatus,
        Set<String> authorities

) {
    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId(),
                user.getLogin(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isActivated(),
                user.getLangKey(),
                user.getCreatedDate(),
                user.getCertificationStatus(),
                user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet()));
    }

    // public static UserDTO from(UserPrincipal u) {
    //     return new UserDTO(
    //             u.id(),
    //             u.login(),
    //             u.firstName(),
    //             u.lastName(),
    //             u.email(),
    //             u.activated(),
    //             u.langKey(),
    //             u.createdDate(),
    //             u.certificationStatus(),
    //             u.authorities());
    // }
}
