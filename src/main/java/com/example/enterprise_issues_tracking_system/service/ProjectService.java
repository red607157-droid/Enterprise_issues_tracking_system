package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.ProjectRequest;
import com.example.enterprise_issues_tracking_system.dto.ProjectResponse;
import com.example.enterprise_issues_tracking_system.entity.Project;
import com.example.enterprise_issues_tracking_system.entity.User;
import com.example.enterprise_issues_tracking_system.exception.DuplicateResourceException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.ProjectMapper;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final ProjectMemberService projectMemberService;
    private final CurrentUserProvider currentUserProvider;

    public ProjectResponse createProject(ProjectRequest request) {
        if (projectRepository.existsByKeyAndDeletedAtIsNull(request.getKey())) {
            throw new DuplicateResourceException("Project key already exists");
        }
        Project project = projectMapper.toEntity(request);
        User owner = currentUserProvider.getCurrentUser();
        project.setOwner(owner);
        projectRepository.save(project);
        projectMemberService.addOwnerAsAdmin(project, owner);
        return projectMapper.toResponse(project);
    }

    public ProjectResponse getProjectById(Long id) {
        return projectMapper.toResponse(findVisibleProject(id));
    }

    public Page<ProjectResponse> getAllProject(Pageable pageable) {
        Long callerId = currentUserProvider.getCurrentUser().getId();
        return projectRepository.findAllVisible(callerId, pageable)
                .map(projectMapper::toResponse);
    }

    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findVisibleProject(id);
        checkCallerIsOwner(project);
        if (projectRepository.existsByKeyAndDeletedAtIsNullAndIdNot(request.getKey(), id)) {
            throw new DuplicateResourceException("Project key already exists");
        }
        Project newProject = projectMapper.updateEntity(request, project);
        projectRepository.save(newProject);
        return projectMapper.toResponse(newProject);
    }

    public void deleteProject(Long id) {
        Project project = findVisibleProject(id);
        checkCallerIsOwner(project);
        project.softDelete();
        projectRepository.save(project);
    }

    private Project findVisibleProject(Long id) {
        Long callerId = currentUserProvider.getCurrentUser().getId();
        return projectRepository.findVisibleById(id, callerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
    }

    private void checkCallerIsOwner(Project project) {
        if (!currentUserProvider.getCurrentUser().equals(project.getOwner())) {
            throw new AccessDeniedException("You don't have the permission to modify this project");
        }
    }
}