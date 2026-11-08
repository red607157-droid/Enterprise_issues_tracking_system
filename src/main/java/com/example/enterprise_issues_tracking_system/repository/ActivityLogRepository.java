package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>, JpaSpecificationExecutor<ActivityLog> {

    Page<ActivityLog> findAllByIssueId(Long issueId, Pageable pageable);
}
