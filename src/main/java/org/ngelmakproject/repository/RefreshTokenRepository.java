package org.ngelmakproject.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.ngelmakproject.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    
    Optional<RefreshToken> findByTokenAndRevokedFalseAndExpiresAtAfter(String token, Instant now);
    
    List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);
}
