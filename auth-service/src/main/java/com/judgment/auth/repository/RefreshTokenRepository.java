package com.judgment.auth.repository;

import com.judgment.auth.entity.RefreshToken;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);
}
