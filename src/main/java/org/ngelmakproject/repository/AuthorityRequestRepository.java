package org.ngelmakproject.repository;

import java.util.List;

import org.ngelmakproject.domain.AuthorityRequest;
import org.ngelmakproject.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityRequestRepository extends JpaRepository<AuthorityRequest, Long> {
  List<AuthorityRequest> findByUser(User user);
}
