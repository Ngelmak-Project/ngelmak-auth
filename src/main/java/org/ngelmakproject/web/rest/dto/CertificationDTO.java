package org.ngelmakproject.web.rest.dto;

import org.ngelmakproject.domain.enumeration.DocType;

public record CertificationDTO(Long id, DocType docType, String docIdentification) {

}
