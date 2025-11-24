package org.ngelmakproject.web.rest.util;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Optional response builder
 */
public interface ResponseUtil {

  static <T> ResponseEntity<T> wrapOrNotFound(Optional<T> entity) {
    return wrapOrNotFound(entity, null);
  }

  static <T> ResponseEntity<T> wrapOrNotFound(Optional<T> entity, HttpHeaders headers) {
    return entity
        .map(response -> ResponseEntity.ok().headers(headers).body(response))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
