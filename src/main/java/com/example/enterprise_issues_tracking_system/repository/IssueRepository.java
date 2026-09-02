package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    Optional<Issue> findByIdAndDeletedAtIsNull(Long id);
}
