package org.ngelmakproject.web.rest.errors;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BadRequestAlertException {

  private static final long serialVersionUID = 1L;

  public ResourceNotFoundException(String defaultMessage, String entityName, String errorKey) {
    super(defaultMessage, entityName, errorKey, HttpStatus.NOT_FOUND);
  }
}
