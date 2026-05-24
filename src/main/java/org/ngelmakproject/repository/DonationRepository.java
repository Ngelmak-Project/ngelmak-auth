package org.ngelmakproject.repository;

import java.util.List;

import org.ngelmakproject.domain.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Donation entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {
  List<Donation> findTop20ByOrderByCreatedAtDesc();
}
