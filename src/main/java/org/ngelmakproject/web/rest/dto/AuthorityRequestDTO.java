package org.ngelmakproject.web.rest.dto;

import java.time.Instant;

import org.ngelmakproject.domain.Authority;
import org.ngelmakproject.domain.AuthorityRequest;
import org.ngelmakproject.domain.AuthorityRequest.RequestStatus;

public record AuthorityRequestDTO(Long id,
        Authority authority,
        String motivation,
        RequestStatus status,
        Instant requestedAt) {
    public static AuthorityRequestDTO from(AuthorityRequest authorityRequest) {
        return new AuthorityRequestDTO(
                authorityRequest.getId(),
                authorityRequest.getAuthority(),
                authorityRequest.getMotivation(),
                authorityRequest.getStatus(),
                authorityRequest.getRequestedAt());
    }
}
