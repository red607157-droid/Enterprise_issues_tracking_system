package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
}
