package org.ngelmakproject.web.rest.dto;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import org.ngelmakproject.domain.Authority;
import org.ngelmakproject.domain.User;
import org.ngelmakproject.domain.enumeration.CertificationStatus;

/**
 * A DTO representing a user, with his authorities.
 */
public record UserDTO(
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
}
