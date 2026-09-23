package com.uade.lime.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.lime.auth.model.DenylistedToken;

@Repository
public interface DenylistedTokenRepository extends JpaRepository<DenylistedToken, Long> {

    boolean existsByJti(String jti);
}
