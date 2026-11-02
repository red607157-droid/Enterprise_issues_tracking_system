package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.LabelRequest;
import com.example.enterprise_issues_tracking_system.dto.LabelResponse;
import com.example.enterprise_issues_tracking_system.dto.LabelUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.LabelMapper;
import com.example.enterprise_issues_tracking_system.repository.LabelRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
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
class LabelServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private LabelMapper labelMapper;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private LabelService labelService;

    private ProjectMember buildMembership(Long projectId, Long userId, Role role) {
        ProjectMemberId id = new ProjectMemberId();
        Project project = new Project();
        project.setId(projectId);
        User user = new User();
        user.setId(userId);
        id.setProject(project);
        id.setUser(user);
        ProjectMember member = new ProjectMember();
        member.setId(id);
        member.setRole(role);
        return member;
    }

    @Test
    public void createLabel_whenCallerIsAdmin_shouldCreate() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        LabelRequest request = new LabelRequest();
        request.setProjectId(projectId);
        Long labelId = 3L;
        Label label = new Label();
        label.setProject(project);
        label.setId(labelId);
        LabelResponse response = new LabelResponse();
        response.setId(labelId);
        response.setProjectId(projectId);

        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.ADMIN)));
        when(labelMapper.toEntity(request)).thenReturn(label);
        when(labelMapper.toResponse(label)).thenReturn(response);
        LabelResponse result = labelService.createLabel(request);

        assertEquals(response, result);
        verify(labelRepository).save(any(Label.class));
    }

    @Test
    public void createLabel_whenCallerIsDeveloper_shouldThrowException() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        LabelRequest request = new LabelRequest();
        request.setProjectId(projectId);

        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));

        assertThrows(AccessDeniedException.class, () -> labelService.createLabel(request));
        verify(labelRepository, never()).save(any());
    }

    @Test
    public void getLabel_whenFound_shouldRespond() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long labelId = 1L;
        Label label = new Label();
        label.setId(labelId);
        label.setProject(project);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        LabelResponse response = new LabelResponse();
        response.setId(labelId);
        response.setProjectId(projectId);

        when(labelRepository.findByIdAndDeletedAtIsNull(labelId)).thenReturn(Optional.of(label));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        when(labelMapper.toResponse(label)).thenReturn(response);
        LabelResponse result = labelService.getLabelById(labelId);

        assertEquals(response, result);
        verify(labelRepository).findByIdAndDeletedAtIsNull(labelId);
    }

    @Test
    public void getLabel_whenNotFound_shouldThrowException() {
        Long labelId = 1L;

        when(labelRepository.findByIdAndDeletedAtIsNull(labelId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> labelService.getLabelById(labelId));
    }

    @Test
    public void getLabel_whenUnauthorized_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long labelId = 1L;
        Label label = new Label();
        label.setId(labelId);
        label.setProject(project);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);

        when(labelRepository.findByIdAndDeletedAtIsNull(labelId)).thenReturn(Optional.of(label));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> labelService.getLabelById(labelId));
    }

    @Test
    public void getAllLabel_whenAuthorized_shouldRespond() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Label label1 = new Label();
        label1.setId(1L);
        label1.setProject(project);
        Label label2 = new Label();
        label2.setId(4L);
        label2.setProject(project);
        LabelResponse response1 = new LabelResponse();
        response1.setId(1L);
        LabelResponse response2 = new LabelResponse();
        response2.setId(4L);
        Pageable pageable = PageRequest.of(0, 10);

        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        when(labelRepository.findAllByProjectIdAndDeletedAtIsNull(projectId, pageable)).thenReturn(new PageImpl<>(List.of(label2, label1)));
        when(labelMapper.toResponse(label1)).thenReturn(response1);
        when(labelMapper.toResponse(label2)).thenReturn(response2);

        Page<LabelResponse> result = labelService.getLabelsByProject(projectId, pageable);

        assertEquals(List.of(response2, response1), result.getContent());
    }

    @Test
    public void getAllLabel_whenUnauthorized_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Pageable pageable = PageRequest.of(0, 10);

        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> labelService.getLabelsByProject(projectId, pageable));
        verify(labelRepository, never()).findAllByProjectIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    public void updateLabel_whenAuthorized_shouldUpdate() {
        String color = "orange";
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long labelId = 1L;
        Label existingLabel = new Label();
        existingLabel.setId(labelId);
        existingLabel.setProject(project);
        existingLabel.setColor("blue");
        LabelUpdateRequest request = new LabelUpdateRequest();
        request.setColor(color);
        Label newLabel = new Label();
        newLabel.setColor(color);
        LabelResponse response = new LabelResponse();
        response.setId(labelId);
        response.setProjectId(projectId);
        response.setColor(color);

        when(labelRepository.findByIdAndDeletedAtIsNull(labelId)).thenReturn(Optional.of(existingLabel));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.ADMIN)));
        when(labelMapper.updateEntity(request, existingLabel)).thenReturn(newLabel);
        when(labelMapper.toResponse(newLabel)).thenReturn(response);
        LabelResponse result = labelService.updateLabel(labelId, request);

        assertEquals(response, result);
        verify(labelRepository).save(any(Label.class));
    }

    @Test
    public void updateLabel_whenUnauthorized_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long labelId = 1L;
        Label existingLabel = new Label();
        existingLabel.setId(labelId);
        existingLabel.setProject(project);
        LabelUpdateRequest request = new LabelUpdateRequest();

        when(labelRepository.findByIdAndDeletedAtIsNull(labelId)).thenReturn(Optional.of(existingLabel));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> labelService.updateLabel(labelId, request));
        verify(labelRepository, never()).save(any());
    }

    @Test
    public void deleteLabel_whenAuthorized_shouldDelete() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long labelId = 1L;
        Label label = new Label();
        label.setId(labelId);
        label.setProject(project);

        when(labelRepository.findByIdAndDeletedAtIsNull(labelId)).thenReturn(Optional.of(label));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.ADMIN)));
        labelService.deleteLabel(labelId);

        verify(labelRepository).save(any(Label.class));
    }

    @Test
    public void deleteLabel_whenUnauthorized_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long labelId = 1L;
        Label label = new Label();
        label.setId(labelId);
        label.setProject(project);

        when(labelRepository.findByIdAndDeletedAtIsNull(labelId)).thenReturn(Optional.of(label));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));

        assertThrows(AccessDeniedException.class, () -> labelService.deleteLabel(labelId));
        verify(labelRepository, never()).save(any());

    }
}
