package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.ProjectRequest;
import com.example.enterprise_issues_tracking_system.dto.ProjectResponse;
import com.example.enterprise_issues_tracking_system.entity.Project;
import com.example.enterprise_issues_tracking_system.entity.User;
import com.example.enterprise_issues_tracking_system.mapper.ProjectMapper;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class       ProjectServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ProjectService projectService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberService projectMemberService;

    @Test
    public void getProjectById_whenProjectExists_shouldRespond() {
        Long id = 1L;
        Project project = new Project();
        project.setId(id);
        when(projectRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(project));

        ProjectResponse expectedResponse = new ProjectResponse();
        expectedResponse.setId(id);
        when(projectMapper.toResponse(project)).thenReturn(expectedResponse);
        ProjectResponse result = projectService.getProjectById(id);

        assertEquals(expectedResponse, result);
        verify(projectRepository).findByIdAndDeletedAtIsNull(id);
    }

    @Test
    public void getProjectById_whenProjectNotExists_shouldThrowExceptions() {
        Long id = 1L;
        when(projectRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> projectService.getProjectById(id));
        verify(projectRepository).findByIdAndDeletedAtIsNull(id);
    }

    @Test
    public void getAllProject_shouldRespond() {
        Pageable pageable = PageRequest.of(0, 10);
        Project project1 = new Project();
        Project project2 = new Project();
        Page<Project> fakePage = new PageImpl<>(List.of(project1, project2));
        when(projectRepository.findAllByDeletedAtIsNull(pageable)).thenReturn(fakePage);
        Page<ProjectResponse> result = projectService.getAllProject(pageable);

        assertEquals(2, result.getContent().size());
        verify(projectRepository).findAllByDeletedAtIsNull(pageable);
    }

    @Test
    public void createProject_shouldRespond() {
        User user = new User();
        user.setId(1L);
        String projectName = "White Room";
        String key = "ABS";
        ProjectRequest request = new ProjectRequest();
        request.setName(projectName);
        request.setKey(key);
        Project project = new Project();
        project.setName(projectName);
        project.setKey(key);
        ProjectResponse expected = new ProjectResponse();
        expected.setOwnerId(1L);
        expected.setName(projectName);
        expected.setKey(key);

        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(projectMapper.toEntity(request)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(expected);
        ProjectResponse result = projectService.createProject(request);


        assertEquals(expected, result);
        verify(projectRepository).save(any(Project.class));
        verify(projectMemberService).addOwnerAsAdmin(any(Project.class), eq(user));

    }

    @Test
    public void updateProject_whenAuthorized_shouldRespond() {
        String projectName = "White Room";
        String key = "ABS";
        Long id = 1L;
        User user = new User();
        user.setId(2L);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        Project project = new Project();
        project.setOwner(user);
        when(projectRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(project));
        ProjectRequest request = new ProjectRequest();
        request.setName(projectName);
        request.setKey(key);
        Project newProject = new Project();
        newProject.setKey(key);
        newProject.setName(projectName);
        when(projectMapper.updateEntity(request, project)).thenReturn(newProject);
        ProjectResponse expected = new ProjectResponse();
        expected.setKey(key);
        expected.setName(projectName);
        expected.setOwnerId(2L);
        when(projectMapper.toResponse(newProject)).thenReturn(expected);

        ProjectResponse result = projectService.updateProject(1L, request);

        assertEquals(expected, result);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    public void updateProject_whenUnauthorized_shouldThrowExceptions() {
        Long id = 1L;
        User owner = new User();
        owner.setId(2L);
        User caller = new User();
        caller.setId(3L);
        Project project = new Project();
        project.setOwner(owner);
        ProjectRequest request = new ProjectRequest();
        when(projectRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);

        assertThrows(AccessDeniedException.class, () -> projectService.updateProject(id, request));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    public void DeleteProject_whenAuthorized_shouldDelete() {
        Long id = 1L;
        User user = new User();
        user.setId(2L);
        Project project = new Project();
        project.setId(id);
        project.setOwner(user);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(projectRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(project));

        projectService.deleteProject(id);

        verify(projectRepository).save(any(Project.class));
    }

    @Test
    public void DeleteProject_whenUnauthorized_shouldThrowExceptions() {
        Long id = 1L;
        User owner = new User();
        owner.setId(2L);
        User caller = new User();
        caller.setId(3L);
        Project project = new Project();
        project.setOwner(owner);
        when(projectRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);

        assertThrows(AccessDeniedException.class, () -> projectService.deleteProject(id));

        verify(projectRepository, never()).save(any(Project.class));
    }

}