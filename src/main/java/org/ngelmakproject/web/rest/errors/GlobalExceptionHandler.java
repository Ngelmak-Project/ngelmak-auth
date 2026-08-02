package org.ngelmakproject.web.rest.errors;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.ngelmakproject.web.rest.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BadRequestAlertException.class)
	public ResponseEntity<Object> handleBadRequestAlertException(BadRequestAlertException ex) {

		Map<String, Object> body = new HashMap<>();
		body.put("timestamp", Instant.now());
		body.put("status", ex.getStatus().value());
		body.put("error", ex.getStatus().getReasonPhrase());
		body.put("message", ex.getMessage());
		body.put("entity", ex.getEntityName());
		body.put("errorKey", ex.getErrorKey());

		return ResponseEntity
				.status(ex.getStatus())
				.body(body);
	}

	    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(TokenExpiredException e) {
        ErrorResponse error = new ErrorResponse(
            "TOKEN_EXPIRED",
            "Your session has expired. Please log in again.",
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        ErrorResponse error = new ErrorResponse(
            "UNAUTHORIZED",
            "Access denied. Invalid credentials or token.",
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
}
