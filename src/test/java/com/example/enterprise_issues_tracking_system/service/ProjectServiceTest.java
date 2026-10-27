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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberService projectMemberService;

    @InjectMocks
    private ProjectService projectService;

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    @Test
    public void getProjectById_whenVisible_shouldRespond() {
        Long id = 1L;
        User caller = buildUser(2L);
        Project project = new Project();
        project.setId(id);
        ProjectResponse expected = new ProjectResponse();
        expected.setId(id);

        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectRepository.findVisibleById(id, 2L)).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project)).thenReturn(expected);

        ProjectResponse result = projectService.getProjectById(id);

        assertEquals(expected, result);
        verify(projectRepository).findVisibleById(id, 2L);
    }

    @Test
    public void getProjectById_whenNotVisible_shouldThrowNotFound() {
        Long id = 1L;

        when(currentUserProvider.getCurrentUser()).thenReturn(buildUser(2L));
        when(projectRepository.findVisibleById(id, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getProjectById(id));
        verify(projectMapper, never()).toResponse(any());
    }


    @Test
    public void getAllProject_shouldReturnOnlyProjectsVisibleToCaller() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> fakePage = new PageImpl<>(List.of(new Project(), new Project()));

        when(currentUserProvider.getCurrentUser()).thenReturn(buildUser(2L));
        when(projectRepository.findAllVisible(2L, pageable)).thenReturn(fakePage);

        Page<ProjectResponse> result = projectService.getAllProject(pageable);

        assertEquals(2, result.getContent().size());
        verify(projectRepository).findAllVisible(2L, pageable);
    }

    @Test
    public void createProject_shouldRespond() {
        User user = buildUser(1L);
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
    public void createProject_whenKeyAlreadyExists_shouldThrowException() {
        String key = "ABS";
        ProjectRequest request = new ProjectRequest();
        request.setName("White Room");
        request.setKey(key);

        when(projectRepository.existsByKeyAndDeletedAtIsNull(key)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> projectService.createProject(request));
        verify(projectRepository, never()).save(any(Project.class));
        verify(projectMemberService, never()).addOwnerAsAdmin(any(), any());
    }

    @Test
    public void updateProject_whenAuthorized_shouldRespond() {
        Long id = 1L;
        User owner = buildUser(2L);
        Project project = new Project();
        project.setId(id);
        project.setOwner(owner);
        ProjectRequest request = new ProjectRequest();
        request.setName("White Room");
        request.setKey("ABS");
        Project newProject = new Project();
        newProject.setKey("ABS");
        newProject.setName("White Room");
        ProjectResponse expected = new ProjectResponse();
        expected.setKey("ABS");
        expected.setName("White Room");
        expected.setOwnerId(2L);

        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findVisibleById(id, 2L)).thenReturn(Optional.of(project));
        when(projectMapper.updateEntity(request, project)).thenReturn(newProject);
        when(projectMapper.toResponse(newProject)).thenReturn(expected);

        ProjectResponse result = projectService.updateProject(id, request);

        assertEquals(expected, result);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    public void updateProject_whenMemberButNotOwner_shouldThrowAccessDenied() {
        Long id = 1L;
        User owner = buildUser(2L);
        User caller = buildUser(3L);
        Project project = new Project();
        project.setId(id);
        project.setOwner(owner);

        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectRepository.findVisibleById(id, 3L)).thenReturn(Optional.of(project));

        assertThrows(AccessDeniedException.class, () -> projectService.updateProject(id, new ProjectRequest()));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    public void updateProject_whenNotVisible_shouldThrowNotFound() {
        Long id = 1L;

        when(currentUserProvider.getCurrentUser()).thenReturn(buildUser(3L));
        when(projectRepository.findVisibleById(id, 3L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.updateProject(id, new ProjectRequest()));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    public void updateProject_whenKeyBelongsToAnotherProject_shouldThrowException() {
        Long id = 1L;
        String key = "ABS";
        User owner = buildUser(2L);
        Project project = new Project();
        project.setId(id);
        project.setOwner(owner);
        ProjectRequest request = new ProjectRequest();
        request.setKey(key);

        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findVisibleById(id, 2L)).thenReturn(Optional.of(project));
        when(projectRepository.existsByKeyAndDeletedAtIsNullAndIdNot(key, id)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> projectService.updateProject(id, request));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    public void deleteProject_whenAuthorized_shouldDelete() {
        Long id = 1L;
        User owner = buildUser(2L);
        Project project = new Project();
        project.setId(id);
        project.setOwner(owner);

        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(projectRepository.findVisibleById(id, 2L)).thenReturn(Optional.of(project));

        projectService.deleteProject(id);

        assertNotNull(project.getDeletedAt());
        verify(projectRepository).save(project);
    }

    @Test
    public void deleteProject_whenMemberButNotOwner_shouldThrowAccessDenied() {
        Long id = 1L;
        User owner = buildUser(2L);
        User caller = buildUser(3L);
        Project project = new Project();
        project.setId(id);
        project.setOwner(owner);

        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectRepository.findVisibleById(id, 3L)).thenReturn(Optional.of(project));

        assertThrows(AccessDeniedException.class, () -> projectService.deleteProject(id));
        assertNull(project.getDeletedAt());
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    public void deleteProject_whenNotVisible_shouldThrowNotFound() {
        Long id = 1L;

        when(currentUserProvider.getCurrentUser()).thenReturn(buildUser(3L));
        when(projectRepository.findVisibleById(id, 3L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.deleteProject(id));
        verify(projectRepository, never()).save(any(Project.class));
    }
}