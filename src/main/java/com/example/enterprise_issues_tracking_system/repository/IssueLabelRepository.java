package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.IssueLabel;
import com.example.enterprise_issues_tracking_system.entity.IssueLabelId;
import com.example.enterprise_issues_tracking_system.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IssueLabelRepository extends JpaRepository<IssueLabel, IssueLabelId> {
    @Query("SELECT il FROM IssueLabel il WHERE il.id.issue.id = :issueId AND il.id.label.id = :labelId")
    Optional<IssueLabel> findLink(@Param("issueId") Long issueId, @Param("labelId") Long labelId);
    @Query("SELECT l FROM IssueLabel il JOIN il.id.label l WHERE il.id.issue.id = :issueId AND l.deletedAt IS NULL")
    List<Label> findActiveLabelsByIssueId(@Param("issueId") Long issueId);
}
