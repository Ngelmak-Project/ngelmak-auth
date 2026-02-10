package org.ngelmakproject.domain.enumeration;

public enum CertificationStatus {
    NOT_REQUESTED, // User has not submitted any certification documents.
    PENDING, // User has submitted certification documents and is awaiting review.
    APPROVED, // User's certification documents have been reviewed and approved.
    REJECTED, // User's certification documents have been reviewed and rejected.
}
