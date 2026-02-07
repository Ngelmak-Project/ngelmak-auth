package org.ngelmakproject.web.rest.errors;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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
}
