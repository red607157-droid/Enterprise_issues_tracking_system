package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT c FROM Comment c WHERE c.issue.id = :issueId "
        + "AND (c.deletedAt IS NULL OR EXISTS(SELECT 1 FROM Comment r WHERE r.parentComment = c))")
    Page<Comment> findVisibleByIssueId(@Param("issueId") Long issueId, Pageable pageable);}
