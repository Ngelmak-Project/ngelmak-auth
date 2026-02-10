package org.ngelmakproject.repository;

import org.ngelmakproject.domain.AuthorityHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityHistoryRepository extends JpaRepository<AuthorityHistory, Long> {
  
}
