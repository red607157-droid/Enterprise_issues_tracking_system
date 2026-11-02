package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.IssueLabel;
import com.example.enterprise_issues_tracking_system.entity.IssueLabelId;
import com.example.enterprise_issues_tracking_system.entity.Label;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IssueLabelRepository extends JpaRepository<IssueLabel, IssueLabelId> {
    @Query("SELECT il FROM IssueLabel il WHERE il.id.issue.id = :issueId AND il.id.label.id = :labelId")
    Optional<IssueLabel> findLink(@Param("issueId") Long issueId, @Param("labelId") Long labelId);
    @Query("SELECT l FROM Label l WHERE l.deletedAt IS NULL "
        + "AND EXISTS (SELECT 1 FROM IssueLabel il WHERE il.id.label = l AND il.id.issue.id = :issueId)")
    Page<Label> findActiveLabelsByIssueId(@Param("issueId") Long issueId, Pageable pageable);
}
