package org.ngelmakproject.web.rest.errors;

import org.springframework.http.HttpStatus;

/**
 * Custom exception to indicate a bad request with additional details.
 */
public class BadRequestAlertException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final HttpStatus status;
	private final String entityName;
	private final String errorKey;

	/**
	 * Constructor for creating a BadRequestAlertException.
	 *
	 * @param defaultMessage the default error message
	 * @param entityName     the name of the entity that caused the error
	 * @param errorKey       a unique key representing the type of error
	 */
	public BadRequestAlertException(String defaultMessage, String entityName, String errorKey) {
		super(defaultMessage);
		this.entityName = entityName;
		this.errorKey = errorKey;
		this.status = HttpStatus.BAD_REQUEST;
	}

		/**
	 * Constructor for creating a BadRequestAlertException.
	 *
	 * @param defaultMessage the default error message
	 * @param entityName     the name of the entity that caused the error
	 * @param errorKey       a unique key representing the type of error
	 * @param status         status of the error
	 */
	public BadRequestAlertException(String defaultMessage, String entityName, String errorKey, HttpStatus status) {
		super(defaultMessage);
		this.entityName = entityName;
		this.errorKey = errorKey;
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getEntityName() {
		return entityName;
	}

	public String getErrorKey() {
		return errorKey;
	}
}
