package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByKeyAndDeletedAtIsNull(String key);
    boolean existsByKeyAndDeletedAtIsNullAndIdNot(String key, Long id);

    @Query("SELECT p FROM Project p WHERE p.id = :projectId AND p.deletedAt IS NULL " +
            "AND EXISTS(SELECT 1 FROM ProjectMember pm WHERE pm.id.project = p AND pm.id.user.id = :userId)")
    Optional<Project> findVisibleById(@Param("projectId") Long id, @Param("userId") Long userId);

    @Query("SELECT p FROM Project p WHERE p.deletedAt IS NULL "
            + "AND EXISTS (SELECT 1 FROM ProjectMember pm WHERE pm.id.project = p AND pm.id.user.id = :userId)")
    Page<Project> findAllVisible(@Param("userId") Long userId, Pageable pageable);
}
