package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.ProjectMemberRequest;
import com.example.enterprise_issues_tracking_system.dto.ProjectMemberResponse;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.DuplicateResourceException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.ProjectMemberMapper;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.repository.UserRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    private static final Long PROJECT_ID = 2L;
    private static final Long CALLER_ID = 3L;
    private static final Long TARGET_ID = 1L;
    private static final Long OWNER_ID = 9L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private Project project;
    private User caller;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(PROJECT_ID);
        project.setOwner(buildUser(OWNER_ID));
        caller = buildUser(CALLER_ID);
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private ProjectMember buildMembership(Project project, User user, Role role) {
        ProjectMemberId id = new ProjectMemberId();
        id.setProject(project);
        id.setUser(user);
        ProjectMember projectMember = new ProjectMember();
        projectMember.setId(id);
        projectMember.setRole(role);
        return projectMember;
    }

    private ProjectMemberRequest buildRequest(Long userId, Role role) {
        ProjectMemberRequest request = new ProjectMemberRequest();
        request.setUserId(userId);
        request.setRole(role);
        return request;
    }

    private void givenActiveProject() {
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
    }

    private void givenCallerRole(Role role) {
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(PROJECT_ID, CALLER_ID))
                .thenReturn(Optional.of(buildMembership(project, caller, role)));
    }

    private void givenCallerHasNoMembership() {
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(PROJECT_ID, CALLER_ID)).thenReturn(Optional.empty());
    }

    @Test
    public void addMember_whenCallerIsAdmin_shouldSucceed() {
        User user = buildUser(TARGET_ID);
        ProjectMemberRequest request = buildRequest(TARGET_ID, Role.DEVELOPER);
        ProjectMember newMember = buildMembership(project, user, Role.DEVELOPER);
        ProjectMemberResponse expected = new ProjectMemberResponse();

        givenActiveProject();
        givenCallerRole(Role.ADMIN);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(user));
        when(projectMemberRepository.findMembership(PROJECT_ID, TARGET_ID)).thenReturn(Optional.empty());
        when(projectMemberMapper.toEntity(request, user, project)).thenReturn(newMember);
        when(projectMemberMapper.toResponse(newMember)).thenReturn(expected);

        ProjectMemberResponse result = projectMemberService.addMember(PROJECT_ID, request);

        assertEquals(expected, result);
        verify(projectMemberRepository).save(newMember);
    }

    @Test
    public void addMember_whenCallerIsDeveloper_shouldThrowException() {
        givenActiveProject();
        givenCallerRole(Role.DEVELOPER);

        assertThrows(AccessDeniedException.class,
                () -> projectMemberService.addMember(PROJECT_ID, buildRequest(TARGET_ID, Role.DEVELOPER)));
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    public void addMember_whenCallerHasNoMembership_shouldThrowException() {
        givenActiveProject();
        givenCallerHasNoMembership();

        assertThrows(AccessDeniedException.class,
                () -> projectMemberService.addMember(PROJECT_ID, buildRequest(TARGET_ID, Role.DEVELOPER)));
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    public void addMember_whenProjectNotFound_shouldThrowException() {
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> projectMemberService.addMember(PROJECT_ID, buildRequest(TARGET_ID, Role.DEVELOPER)));
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    public void addMember_whenManagerGrantsAdmin_shouldThrowException() {
        givenActiveProject();
        givenCallerRole(Role.PROJECT_MANAGER);

        assertThrows(AccessDeniedException.class,
                () -> projectMemberService.addMember(PROJECT_ID, buildRequest(TARGET_ID, Role.ADMIN)));
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    public void addMember_whenAlreadyMember_shouldThrowException() {
        User user = buildUser(TARGET_ID);

        givenActiveProject();
        givenCallerRole(Role.ADMIN);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(user));
        when(projectMemberRepository.findMembership(PROJECT_ID, TARGET_ID))
                .thenReturn(Optional.of(buildMembership(project, user, Role.DEVELOPER)));

        assertThrows(DuplicateResourceException.class,
                () -> projectMemberService.addMember(PROJECT_ID, buildRequest(TARGET_ID, Role.REPORTER)));
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    public void getMember_whenFound_shouldRespond() {
        ProjectMember target = buildMembership(project, buildUser(TARGET_ID), Role.DEVELOPER);
        ProjectMemberResponse expected = new ProjectMemberResponse();

        givenActiveProject();
        givenCallerRole(Role.PROJECT_MANAGER);
        when(projectMemberRepository.findMembership(PROJECT_ID, TARGET_ID)).thenReturn(Optional.of(target));
        when(projectMemberMapper.toResponse(target)).thenReturn(expected);

        ProjectMemberResponse result = projectMemberService.getMember(PROJECT_ID, TARGET_ID);

        assertEquals(expected, result);
    }

    @Test
    public void getMember_whenCallerIsDeveloper_shouldThrowException() {
        givenActiveProject();
        givenCallerRole(Role.DEVELOPER);

        assertThrows(AccessDeniedException.class, () -> projectMemberService.getMember(PROJECT_ID, TARGET_ID));
    }

    @Test
    public void getMember_whenProjectNotFound_shouldThrowException() {
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectMemberService.getMember(PROJECT_ID, TARGET_ID));
        verify(projectMemberRepository, never()).findMembership(any(), any());
    }

    // ---------- updateMember ----------

    @Test
    public void updateMember_whenAuthorized_shouldUpdateRole() {
        ProjectMember target = buildMembership(project, buildUser(TARGET_ID), Role.DEVELOPER);
        ProjectMemberRequest request = buildRequest(TARGET_ID, Role.PROJECT_MANAGER);
        ProjectMemberResponse expected = new ProjectMemberResponse();

        givenActiveProject();
        givenCallerRole(Role.ADMIN);
        when(projectMemberRepository.findMembership(PROJECT_ID, TARGET_ID)).thenReturn(Optional.of(target));
        when(projectMemberMapper.toResponse(target)).thenReturn(expected);

        ProjectMemberResponse result = projectMemberService.updateMember(PROJECT_ID, TARGET_ID, request);

        assertEquals(expected, result);
        assertEquals(Role.PROJECT_MANAGER, target.getRole());
        verify(projectMemberRepository).save(target);
    }

    @Test
    public void updateMember_whenManagerTargetsAdmin_shouldThrowException() {
        ProjectMember target = buildMembership(project, buildUser(TARGET_ID), Role.ADMIN);

        givenActiveProject();
        givenCallerRole(Role.PROJECT_MANAGER);
        when(projectMemberRepository.findMembership(PROJECT_ID, TARGET_ID)).thenReturn(Optional.of(target));

        assertThrows(AccessDeniedException.class,
                () -> projectMemberService.updateMember(PROJECT_ID, TARGET_ID, buildRequest(TARGET_ID, Role.DEVELOPER)));
        assertEquals(Role.ADMIN, target.getRole());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    public void updateMember_whenTargetIsOwner_shouldThrowException() {
        ProjectMember ownerMembership = buildMembership(project, buildUser(OWNER_ID), Role.ADMIN);

        givenActiveProject();
        givenCallerRole(Role.ADMIN);
        when(projectMemberRepository.findMembership(PROJECT_ID, OWNER_ID)).thenReturn(Optional.of(ownerMembership));

        assertThrows(AccessDeniedException.class,
                () -> projectMemberService.updateMember(PROJECT_ID, OWNER_ID, buildRequest(OWNER_ID, Role.DEVELOPER)));
        assertEquals(Role.ADMIN, ownerMembership.getRole());
        verify(projectMemberRepository, never()).save(any());
    }

    // ---------- deleteMember ----------

    @Test
    public void deleteMember_whenAuthorized_shouldDelete() {
        ProjectMember target = buildMembership(project, buildUser(TARGET_ID), Role.DEVELOPER);

        givenActiveProject();
        givenCallerRole(Role.ADMIN);
        when(projectMemberRepository.findMembership(PROJECT_ID, TARGET_ID)).thenReturn(Optional.of(target));

        projectMemberService.deleteMember(PROJECT_ID, TARGET_ID);

        verify(projectMemberRepository).delete(target);
    }

    @Test
    public void deleteMember_whenTargetIsOwner_shouldThrowException() {
        ProjectMember ownerMembership = buildMembership(project, buildUser(OWNER_ID), Role.ADMIN);

        givenActiveProject();
        givenCallerRole(Role.ADMIN);
        when(projectMemberRepository.findMembership(PROJECT_ID, OWNER_ID)).thenReturn(Optional.of(ownerMembership));

        assertThrows(AccessDeniedException.class, () -> projectMemberService.deleteMember(PROJECT_ID, OWNER_ID));
        verify(projectMemberRepository, never()).delete(any());
    }

    @Test
    public void deleteMember_whenManagerTargetsAdmin_shouldThrowException() {
        ProjectMember target = buildMembership(project, buildUser(TARGET_ID), Role.ADMIN);

        givenActiveProject();
        givenCallerRole(Role.PROJECT_MANAGER);
        when(projectMemberRepository.findMembership(PROJECT_ID, TARGET_ID)).thenReturn(Optional.of(target));

        assertThrows(AccessDeniedException.class, () -> projectMemberService.deleteMember(PROJECT_ID, TARGET_ID));
        verify(projectMemberRepository, never()).delete(any());
    }
}