package com.example.enterprise_issues_tracking_system.mapper;

import com.example.enterprise_issues_tracking_system.dto.ProjectRequest;
import com.example.enterprise_issues_tracking_system.dto.ProjectResponse;
import com.example.enterprise_issues_tracking_system.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {
    public ProjectResponse toResponse(Project project) {
        ProjectResponse response = new ProjectResponse();

        response.setId(project.getId());
        response.setName(project.getName());
        response.setKey(project.getKey());
        response.setDescription(project.getDescription());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        response.setOwnerId(project.getOwner().getId());
        
        return response;
    }

    public Project toEntity(ProjectRequest request) {
        Project project = new Project();

        project.setKey(request.getKey());
        project.setName(request.getName());
        project.setDescription(request.getDescription());

        return project;
    }

    public Project updateEntity(ProjectRequest request, Project existingProject) {
        existingProject.setKey(request.getKey());
        existingProject.setName(request.getName());
        existingProject.setDescription(request.getDescription());
        return existingProject;
    }
}
