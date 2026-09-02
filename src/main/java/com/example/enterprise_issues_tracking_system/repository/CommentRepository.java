package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);
}
