package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.ProjectMemberRequest;
import com.example.enterprise_issues_tracking_system.dto.ProjectMemberResponse;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.mapper.ProjectMemberMapper;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private ProjectMember buildMembership(Project project, User user, Role role) {
        ProjectMemberId id = new ProjectMemberId();
        id.setProject(project);
        id.setUser(user);
        ProjectMember projectMember = new ProjectMember();
        projectMember.setRole(role);
        projectMember.setId(id);
        return projectMember;
    }

    @Test
    public void addMember_whenCallerIsAdmin_shouldSucceed() {
        Long userId = 1L;
        Long projectId = 2L;
        Long callerId = 3L;
        Project project = new Project();
        project.setId(projectId);
        User user = new User();
        user.setId(userId);
        User caller = new User();
        caller.setId(callerId);
        ProjectMemberId callerMembershipId = new ProjectMemberId();
        callerMembershipId.setUser(caller);
        callerMembershipId.setProject(project);
        ProjectMember callerMembership = new ProjectMember();
        callerMembership.setId(callerMembershipId);
        callerMembership.setRole(Role.ADMIN);
        ProjectMember projectMember = buildMembership(project, user, Role.DEVELOPER);
        ProjectMemberRequest request = new ProjectMemberRequest();
        request.setUserId(userId);
        request.setRole(Role.DEVELOPER);
        ProjectMemberResponse expected = new ProjectMemberResponse();
        expected.setProjectId(projectId);
        expected.setUserId(userId);
        expected.setRole(request.getRole());
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(callerMembership));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberMapper.toEntity(request, user, project)). thenReturn(projectMember);
        when(projectMemberMapper.toResponse(projectMember)).thenReturn(expected);

        ProjectMemberResponse result = projectMemberService.addMember(projectId, request);

        assertEquals(expected, result);
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    public void addMember_whenCallerIsDeveloper_shouldThrowExceptions() {
        Long projectId = 1L;
        Long callerId = 2L;
        Project project = new Project();
        project.setId(projectId);
        User caller = new User();
        caller.setId(callerId);
        ProjectMemberId callerMembershipId = new ProjectMemberId();
        callerMembershipId.setUser(caller);
        callerMembershipId.setProject(project);
        ProjectMember callerMembership = new ProjectMember();
        callerMembership.setId(callerMembershipId);
        callerMembership.setRole(Role.DEVELOPER);
        ProjectMemberRequest request = new ProjectMemberRequest();
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(callerMembership));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));

        assertThrows(AccessDeniedException.class, () -> projectMemberService.addMember(projectId, request));
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    public void addMember_whenCallerHasNoMembership_shouldThrowExceptions() {
        Long projectId = 1L;
        Long callerId = 2L;
        ProjectMemberRequest request = new ProjectMemberRequest();
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> projectMemberService.addMember(projectId, request));
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    public void getMember_whenFound_shouldRespond() {
        Long userId = 1L;
        Long projectId = 2L;
        Long callerId = 3L;
        Project project = new Project();
        project.setId(projectId);
        User user = new User();
        user.setId(userId);
        User caller = new User();
        caller.setId(callerId);
        ProjectMemberId callerMembershipId = new ProjectMemberId();
        callerMembershipId.setUser(caller);
        callerMembershipId.setProject(project);
        ProjectMember callerMembership = new ProjectMember();
        callerMembership.setId(callerMembershipId);
        callerMembership.setRole(Role.DEVELOPER);
        ProjectMember projectMember = buildMembership(project, user, Role.DEVELOPER);
        ProjectMemberResponse expected = new ProjectMemberResponse();
        expected.setProjectId(projectId);
        expected.setUserId(userId);
        expected.setRole(Role.DEVELOPER);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(callerMembership));
        when(projectMemberRepository.findMembership(projectId, userId)).thenReturn(Optional.of(projectMember));
        when(projectMemberMapper.toResponse(projectMember)).thenReturn(expected);

        ProjectMemberResponse result = projectMemberService.getMember(projectId, userId);

        assertEquals(expected, result);
        verify(projectMemberRepository).findMembership(projectId, callerId);
        verify(projectMemberRepository).findMembership(projectId, userId);
    }

    @Test
    public void updateMember_whenAuthorized_shouldUpdateRole() {
        Long userId = 1L;
        Long projectId = 2L;
        Long callerId = 3L;
        Project project = new Project();
        project.setId(projectId);
        User user = new User();
        user.setId(userId);
        User caller = new User();
        caller.setId(callerId);
        ProjectMemberId callerMembershipId = new ProjectMemberId();
        callerMembershipId.setUser(caller);
        callerMembershipId.setProject(project);
        ProjectMember callerMembership = new ProjectMember();
        callerMembership.setId(callerMembershipId);
        callerMembership.setRole(Role.ADMIN);
        ProjectMember projectMember = buildMembership(project, user, Role.DEVELOPER);
        ProjectMemberRequest request = new ProjectMemberRequest();
        request.setUserId(userId);
        request.setRole(Role.PROJECT_MANAGER);
        ProjectMemberResponse expected = new ProjectMemberResponse();
        expected.setProjectId(projectId);
        expected.setUserId(userId);
        expected.setRole(request.getRole());
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(callerMembership));
        when(projectMemberRepository.findMembership(projectId, userId)).thenReturn(Optional.of(projectMember));
        when(projectMemberMapper.toResponse(projectMember)).thenReturn(expected);

        ProjectMemberResponse result = projectMemberService.updateMember(projectId, userId, request);

        assertEquals(expected, result);
        verify(projectMemberRepository).findMembership(projectId, callerId);
        verify(projectMemberRepository).findMembership(projectId, userId);
        verify(projectMemberRepository).save(any(ProjectMember.class));

        ArgumentCaptor<ProjectMember> projectMemberArgumentCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(projectMemberArgumentCaptor.capture());
        assertEquals(Role.PROJECT_MANAGER, projectMemberArgumentCaptor.getValue().getRole());
    }

    @Test
    public void deleteMember_whenAuthorized_shouldDelete() {
        Long userId = 1L;
        Long projectId = 2L;
        Long callerId = 3L;
        Project project = new Project();
        project.setId(projectId);
        User user = new User();
        user.setId(userId);
        User caller = new User();
        caller.setId(callerId);
        ProjectMemberId callerMembershipId = new ProjectMemberId();
        callerMembershipId.setUser(caller);
        callerMembershipId.setProject(project);
        ProjectMember callerMembership = new ProjectMember();
        callerMembership.setId(callerMembershipId);
        callerMembership.setRole(Role.ADMIN);
        ProjectMember projectMember = buildMembership(project, user, Role.DEVELOPER);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(callerMembership));
        when(projectMemberRepository.findMembership(projectId, userId)).thenReturn(Optional.of(projectMember));

        projectMemberService.deleteMember(projectId, userId);

        verify(projectMemberRepository).findMembership(projectId, callerId);
        verify(projectMemberRepository).findMembership(projectId, userId);
        verify(projectMemberRepository).delete(any(ProjectMember.class));
    }
}