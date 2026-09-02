package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.ProjectRequest;
import com.example.enterprise_issues_tracking_system.dto.ProjectResponse;
import com.example.enterprise_issues_tracking_system.entity.Project;
import com.example.enterprise_issues_tracking_system.entity.User;
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
         Project project = projectMapper.toEntity(request);
         User owner = currentUserProvider.getCurrentUser();
         project.setOwner(owner);
         projectRepository.save(project);
         projectMemberService.addOwnerAsAdmin(project, owner);
         return projectMapper.toResponse(project);
     }

     public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        return projectMapper.toResponse(project);
     }

     public Page<ProjectResponse> getAllProject(Pageable pageable) {
        return projectRepository.findAllByDeletedAtIsNull(pageable)
                .map(projectMapper :: toResponse);
     }

     public ProjectResponse updateProject(Long id, ProjectRequest request) {
         Project project = projectRepository.findByIdAndDeletedAtIsNull(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Project ID not found "));
         if (!currentUserProvider.getCurrentUser().equals(project.getOwner())) {
             throw new AccessDeniedException("You don't have the permission to modify this project");
         }
         Project newProject = projectMapper.updateEntity(request, project);
         projectRepository.save(newProject);
         return projectMapper.toResponse(newProject);
     }

     public void deleteProject(Long id) {
         Project project = projectRepository.findByIdAndDeletedAtIsNull(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
         if (!currentUserProvider.getCurrentUser().equals(project.getOwner())) {
             throw new AccessDeniedException("You don't have the permission to modify this project");
         }
         project.softDelete();
         projectRepository.save(project);
     }

}
