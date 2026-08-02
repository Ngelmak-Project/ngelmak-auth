package org.ngelmakproject.web.rest.dto;

public record ErrorResponse(
        String errorCode,
        String message,
        Long timestamp) {
}