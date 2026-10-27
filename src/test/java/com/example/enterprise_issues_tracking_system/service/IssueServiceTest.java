package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.IssueRequest;
import com.example.enterprise_issues_tracking_system.dto.IssueResponse;
import com.example.enterprise_issues_tracking_system.dto.IssueUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.ImmutableFieldException;
import com.example.enterprise_issues_tracking_system.exception.InvalidStatusTransitionException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.IssueMapper;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private IssueMapper issueMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private IssueService issueService;

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
    public void createIssue_whenCallerIsAssignee_shouldCreate() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        ProjectMember callerMembership = buildMembership(projectId, callerId, Role.DEVELOPER);
        Long assigneeId = 3L;
        User assignee = new User();
        assignee.setId(assigneeId);
        ProjectMember assigneeMembership = buildMembership(projectId, assigneeId, Role.DEVELOPER);
        IssueRequest request = new IssueRequest();
        request.setAssigneeId(assigneeId);
        request.setProjectId(projectId);
        Issue issue = new Issue();
        issue.setAssignee(assignee);
        issue.setProject(project);
        IssueResponse response = new IssueResponse();
        response.setAssigneeId(assigneeId);
        response.setProjectId(projectId);

        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(callerMembership));
        when(projectMemberRepository.findMembership(projectId, assigneeId)).thenReturn(Optional.of(assigneeMembership));
        when(issueMapper.toEntity(request)).thenReturn(issue);
        when(issueMapper.toResponse(issue)).thenReturn(response);
        IssueResponse result = issueService.createIssue(request);

        assertEquals(response, result);
        verify(projectRepository).findByIdAndDeletedAtIsNull(projectId);
        verify(projectMemberRepository).findMembership(projectId, callerId);
        verify(projectMemberRepository).findMembership(projectId, assigneeId);
        verify(issueRepository).save(any(Issue.class));

        ArgumentCaptor<Issue> issueArgumentCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueArgumentCaptor.capture());
        assertEquals(IssueStatus.OPEN, issueArgumentCaptor.getValue().getIssueStatus());
    }

    @Test
    public void createIssue_whenAssigneeIsNotAMember_shouldThrowException() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        ProjectMember callerMembership = buildMembership(projectId, callerId, Role.DEVELOPER);
        Long assigneeId = 3L;
        User assignee = new User();
        assignee.setId(assigneeId);
        IssueRequest request = new IssueRequest();
        request.setAssigneeId(assigneeId);
        request.setProjectId(projectId);

        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(callerMembership));
        when(projectMemberRepository.findMembership(projectId, assigneeId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> issueService.createIssue(request));
        verify(issueRepository, never()).save(any());
    }

    @Test
    public void createIssue_whenCallerIsNotAMember_shouldThrowException() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        IssueRequest request = new IssueRequest();
        request.setProjectId(projectId);

        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> issueService.createIssue(request));
        verify(issueRepository, never()).save(any());
    }

    @Test
    public void getIssue_whenFound_shouldRespond() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long issueId = 3L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        ProjectMember callerMembership = buildMembership(projectId, callerId, Role.DEVELOPER);
        IssueResponse response = new IssueResponse();
        response.setProjectId(projectId);
        response.setId(issueId);

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(callerMembership));
        when(issueMapper.toResponse(issue)).thenReturn(response);
        IssueResponse result = issueService.getIssueById(issueId);

        assertEquals(response, result);
        verify(issueRepository).findByIdAndDeletedAtIsNull(issueId);
        verify(projectMemberRepository).findMembership(projectId, callerId);
    }

    @Test
    public void getIssue_whenNotFound_shouldThrowException() {
        Long issueId = 1L;

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> issueService.getIssueById(issueId));
        verify(issueRepository).findByIdAndDeletedAtIsNull(issueId);
    }

    @Test
    public void getIssue_whenUnauthorized_shouldThrowException() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long issueId = 3L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> issueService.getIssueById(issueId));
        verify(issueRepository).findByIdAndDeletedAtIsNull(issueId);
        verify(projectMemberRepository).findMembership(projectId, callerId);
    }

    @Test
    public void getAllIssue_whenFound_shouldRespond() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        ProjectMember callerMembership = buildMembership(projectId, callerId, Role.DEVELOPER);
        Pageable pageable = PageRequest.of(0, 10);
        Issue issue1 = new Issue();
        Issue issue2 = new Issue();
        Page<Issue> fakePage = new PageImpl<>(List.of(issue1, issue2));

        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(callerMembership));
        when(issueRepository.findAllByProjectIdAndDeletedAtIsNull(projectId, pageable)).thenReturn(fakePage);
        Page<IssueResponse> result = issueService.getIssuesByProject(projectId, pageable);

        assertEquals(2, result.getContent().size());
        verify(projectMemberRepository).findMembership(projectId, callerId);
        verify(issueRepository).findAllByProjectIdAndDeletedAtIsNull(projectId, pageable);
    }

    @Test
    public void getAllIssue_whenUnauthorized_shouldThrowException() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> issueService.getIssuesByProject(projectId, pageable));
        verify(issueRepository, never()).findAllByProjectIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    public void updateIssue_whenCallerIsAdminOrManager_shouldUpdate() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long existingIssueId = 1L;
        Issue existingIssue = new Issue();
        existingIssue.setId(existingIssueId);
        existingIssue.setProject(project);
        existingIssue.setIssueStatus(IssueStatus.OPEN);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        ProjectMember callerMembership = buildMembership(projectId, callerId, Role.ADMIN);
        IssueUpdateRequest request = new IssueUpdateRequest();
        request.setDueDate(LocalDateTime.now());
        request.setIssueStatus(IssueStatus.OPEN);
        request.setProjectId(projectId);
        Issue newIssue = new Issue();
        newIssue.setDueDate(LocalDateTime.now());
        newIssue.setId(existingIssueId);
        newIssue.setProject(project);
        IssueResponse response = new IssueResponse();
        response.setDueDate(LocalDateTime.now());
        response.setId(existingIssueId);
        response.setProjectId(projectId);

        when(issueRepository.findByIdAndDeletedAtIsNull(existingIssueId)).thenReturn(Optional.of(existingIssue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(callerMembership));
        when(issueMapper.updateEntity(request, existingIssue)).thenReturn(newIssue);
        when(issueMapper.toResponse(newIssue)).thenReturn(response);
        IssueResponse result = issueService.updateIssue(existingIssueId, request);

        assertEquals(response, result);
        verify(issueRepository).findByIdAndDeletedAtIsNull(existingIssueId);
        verify(projectMemberRepository).findMembership(projectId, callerId);
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    public void updateIssue_whenCallerIsAssignee_shouldUpdate() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        User assignee = new User();
        assignee.setId(callerId);
        Long reporterId = 4L;
        User reporter = new User();
        reporter.setId(reporterId);
        LocalDateTime dueDate = LocalDateTime.now();
        Long existingIssueId = 1L;
        Issue existingIssue = new Issue();
        existingIssue.setId(existingIssueId);
        existingIssue.setProject(project);
        existingIssue.setAssignee(assignee);
        existingIssue.setReporter(reporter);
        existingIssue.setDueDate(dueDate);
        existingIssue.setIssuePriority(IssuePriority.CRITICAL);
        existingIssue.setIssueStatus(IssueStatus.OPEN);
        IssueUpdateRequest request = new IssueUpdateRequest();
        request.setAssigneeId(callerId);
        request.setProjectId(projectId);
        request.setDueDate(dueDate);
        request.setIssuePriority(IssuePriority.LOW);
        request.setIssueStatus(IssueStatus.IN_PROGRESS);
        Issue newIssue = new Issue();
        newIssue.setAssignee(assignee);
        newIssue.setReporter(reporter);
        newIssue.setProject(project);
        newIssue.setDueDate(dueDate);
        newIssue.setIssuePriority(IssuePriority.LOW);
        IssueResponse response = new IssueResponse();
        response.setAssigneeId(callerId);
        response.setReporterId(reporterId);
        response.setProjectId(projectId);
        response.setDueDate(dueDate);
        response.setIssuePriority(IssuePriority.LOW);


        when(issueRepository.findByIdAndDeletedAtIsNull(existingIssueId)).thenReturn(Optional.of(existingIssue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        when(issueMapper.updateEntity(request, existingIssue)).thenReturn(newIssue);
        when(issueMapper.toResponse(newIssue)).thenReturn(response);
        IssueResponse result = issueService.updateIssue(existingIssueId, request);

        assertEquals(response, result);
        verify(issueRepository).findByIdAndDeletedAtIsNull(existingIssueId);
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    public void updateIssue_whenAssigneeChangesDueDate_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        LocalDateTime dueDate = LocalDateTime.now();
        Long existingIssueId = 1L;
        Issue existingIssue = new Issue();
        existingIssue.setId(existingIssueId);
        existingIssue.setProject(project);
        existingIssue.setAssignee(caller);
        existingIssue.setDueDate(dueDate);
        IssueUpdateRequest request = new IssueUpdateRequest();
        request.setProjectId(projectId);
        request.setAssigneeId(callerId);
        request.setDueDate(dueDate.plusDays(1));

        when(issueRepository.findByIdAndDeletedAtIsNull(existingIssueId)).thenReturn(Optional.of(existingIssue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));

        assertThrows(AccessDeniedException.class, () -> issueService.updateIssue(existingIssueId, request));
        verify(issueRepository, never()).save(any());
    }

    @Test
    public void updateIssue_whenIssueIsUnassigned_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long existingIssueId = 1L;
        Issue existingIssue = new Issue();
        existingIssue.setId(existingIssueId);
        existingIssue.setProject(project);
        IssueUpdateRequest request = new IssueUpdateRequest();

        when(issueRepository.findByIdAndDeletedAtIsNull(existingIssueId)).thenReturn(Optional.of(existingIssue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);

        assertThrows(AccessDeniedException.class, () -> issueService.updateIssue(existingIssueId, request));
        verify(issueRepository).findByIdAndDeletedAtIsNull(existingIssueId);
        verify(issueRepository, never()).save(any());
    }

    @Test
    public void updateIssue_whenCallerIsNotMember_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long assigneeId = 4L;
        User assignee = new User();
        assignee.setId(assigneeId);
        Long existingIssueId = 1L;
        Issue existingIssue = new Issue();
        existingIssue.setId(existingIssueId);
        existingIssue.setProject(project);
        existingIssue.setAssignee(assignee);
        IssueUpdateRequest request = new IssueUpdateRequest();

        when(issueRepository.findByIdAndDeletedAtIsNull(existingIssueId)).thenReturn(Optional.of(existingIssue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> issueService.updateIssue(existingIssueId, request));
        verify(issueRepository).findByIdAndDeletedAtIsNull(existingIssueId);
        verify(issueRepository, never()).save(any());
    }

    @Test
    public void updateIssue_whenProjectChanged_shouldThrowExceptionEvenForAdmin() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long existingIssueId = 1L;
        Issue existingIssue = new Issue();
        existingIssue.setId(existingIssueId);
        existingIssue.setProject(project);
        IssueUpdateRequest request = new IssueUpdateRequest();
        request.setProjectId(4L);

        when(issueRepository.findByIdAndDeletedAtIsNull(existingIssueId)).thenReturn(Optional.of(existingIssue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.ADMIN)));

        assertThrows(ImmutableFieldException.class, () -> issueService.updateIssue(existingIssueId, request));
        verify(issueRepository, never()).save(any());
    }

    @Test
    public void updateIssue_whenManagerAssignsNonMember_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long newAssigneeId = 5L;
        Long existingIssueId = 1L;
        Issue existingIssue = new Issue();
        existingIssue.setId(existingIssueId);
        existingIssue.setProject(project);
        IssueUpdateRequest request = new IssueUpdateRequest();
        request.setProjectId(projectId);
        request.setAssigneeId(newAssigneeId);

        when(issueRepository.findByIdAndDeletedAtIsNull(existingIssueId)).thenReturn(Optional.of(existingIssue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.PROJECT_MANAGER)));
        when(projectMemberRepository.findMembership(projectId, newAssigneeId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> issueService.updateIssue(existingIssueId, request));
        verify(issueRepository, never()).save(any());
    }

    @Test
    public void updateIssue_whenStatusTransitionIsInvalid_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long existingIssueId = 1L;
        Issue existingIssue = new Issue();
        existingIssue.setId(existingIssueId);
        existingIssue.setProject(project);
        existingIssue.setIssueStatus(IssueStatus.OPEN);
        IssueUpdateRequest request = new IssueUpdateRequest();
        request.setProjectId(projectId);
        request.setIssueStatus(IssueStatus.DONE);

        when(issueRepository.findByIdAndDeletedAtIsNull(existingIssueId)).thenReturn(Optional.of(existingIssue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.ADMIN)));

        assertThrows(InvalidStatusTransitionException.class, () -> issueService.updateIssue(existingIssueId, request));
        verify(issueRepository, never()).save(any());
    }

    @Test
    public void deleteIssue_whenAuthorized_shouldDelete() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long issueId = 3L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        ProjectMember callerMembership = buildMembership(projectId, callerId, Role.ADMIN);

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(callerMembership));
        issueService.deleteIssue(issueId);

        verify(issueRepository).findByIdAndDeletedAtIsNull(issueId);
        verify(projectMemberRepository).findMembership(projectId, callerId);
        verify(issueRepository).save(any(Issue.class));

        ArgumentCaptor<Issue> issueArgumentCaptor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(issueArgumentCaptor.capture());
        assertNotEquals(null, issueArgumentCaptor.getValue().getDeletedAt());
    }

    @Test
    public void deleteIssue_whenUnauthorized_shouldThrowException() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long issueId = 3L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> issueService.deleteIssue(issueId));
        verify(issueRepository, never()).save(any());
    }
}