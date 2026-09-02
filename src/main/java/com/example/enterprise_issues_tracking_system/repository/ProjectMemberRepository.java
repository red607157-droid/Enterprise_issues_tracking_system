package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.ProjectMember;
import com.example.enterprise_issues_tracking_system.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    @Query("SELECT PM FROM ProjectMember PM WHERE PM.id.project.id = :projectId AND PM.id.user.id = :userId")
    Optional<ProjectMember> findMembership(@Param("projectId") Long projectId,@Param("userId") Long userId);
}
