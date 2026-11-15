package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.dto.BurndownEvent;
import com.example.enterprise_issues_tracking_system.entity.ActivityLog;
import com.example.enterprise_issues_tracking_system.entity.AuditAction;
import com.example.enterprise_issues_tracking_system.entity.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>, JpaSpecificationExecutor<ActivityLog> {

    Page<ActivityLog> findAllByIssueId(Long issueId, Pageable pageable);

    @Query("SELECT new com.example.enterprise_issues_tracking_system.dto.BurndownEvent(a.issueId, a.action, a.details, a.createdAt) "
        + "FROM ActivityLog a "
        + "WHERE a.projectId = :projectId AND a.entityType = :entityType "
        + "AND a.action IN :actions AND a.createdAt < :upTo "
        + "ORDER BY a.createdAt ASC, a.id ASC")
    List<BurndownEvent> findBurnDownEvents(@Param("projectId")Long projectId, @Param("entityType")AuditEntityType entityType,
                                           @Param("actions") Collection<AuditAction> actions, @Param("upTo")LocalDateTime upTo);
}
