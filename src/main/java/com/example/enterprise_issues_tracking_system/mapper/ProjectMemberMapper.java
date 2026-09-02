package com.example.enterprise_issues_tracking_system.mapper;

import com.example.enterprise_issues_tracking_system.dto.ProjectMemberRequest;
import com.example.enterprise_issues_tracking_system.dto.ProjectMemberResponse;
import com.example.enterprise_issues_tracking_system.entity.Project;
import com.example.enterprise_issues_tracking_system.entity.ProjectMember;
import com.example.enterprise_issues_tracking_system.entity.ProjectMemberId;
import com.example.enterprise_issues_tracking_system.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProjectMemberMapper {

    public ProjectMember toEntity(ProjectMemberRequest request, User user, Project project) {
        ProjectMemberId projectMemberId = new ProjectMemberId();

        projectMemberId.setUser(user);
        projectMemberId.setProject(project);


        ProjectMember projectMember = new ProjectMember();

        projectMember.setId(projectMemberId);
        projectMember.setRole(request.getRole());
        projectMember.setCreatedAt(LocalDateTime.now());

        return projectMember;
    }

    public ProjectMemberResponse toResponse(ProjectMember projectMember) {
        ProjectMemberResponse response = new ProjectMemberResponse();

        response.setProjectId(projectMember.getId().getProject().getId());
        response.setUserId(projectMember.getId().getUser().getId());
        response.setRole(projectMember.getRole());
        response.setCreatedAt(projectMember.getCreatedAt());

        return response;
    }
}
