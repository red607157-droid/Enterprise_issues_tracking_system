package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.Issue;
import com.example.enterprise_issues_tracking_system.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {
    Optional<Issue> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT i.issueStatus, COUNT(i) FROM Issue i "+
            "WHERE i.project.id = :projectId AND i.deletedAt IS NULL "+
            "GROUP BY i.issueStatus")
    List<Object[]> countByStatus(@Param("projectId") Long projectId);

    @Query("SELECT a.id , a.fullName, COUNT(i), SUM(CASE WHEN i.issueStatus = :done THEN 1 ELSE 0 END) "+
            "FROM Issue i LEFT JOIN i.assignee a "+
            "WHERE i.project.id = :projectId AND i.deletedAt IS NULL " +
            "GROUP BY a.id, a.fullName")
    List<Object[]> countByAssignee(@Param("projectId") Long projectId, @Param("done")IssueStatus done);

    List<Issue> findAllByProjectIdAndAssigneeIdAndDeletedAtIsNull(Long projectId, Long assigneeId);
}
