package org.ngelmakproject.repository;

import org.ngelmakproject.domain.AuthorityRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityRequestRepository extends JpaRepository<AuthorityRequest, Long> {
  
}
