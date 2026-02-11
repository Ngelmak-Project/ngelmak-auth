package org.ngelmakproject.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.ngelmakproject.domain.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ContactMessage entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
	@Query("""
			SELECT c FROM ContactMessage c
			WHERE c.status != 'CLOSED'
			ORDER BY c.createdAt DESC
			""")
	Page<ContactMessage> findUnclosedContactMessageOrderByCreatedAt(Pageable pageable);
}
