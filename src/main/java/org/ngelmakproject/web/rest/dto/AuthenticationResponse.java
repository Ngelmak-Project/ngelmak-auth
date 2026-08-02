package org.ngelmakproject.web.rest.dto;

import org.ngelmakproject.domain.RefreshToken;

public record AuthenticationResponse(Long userId, String accessToken, RefreshToken rt) {
}
