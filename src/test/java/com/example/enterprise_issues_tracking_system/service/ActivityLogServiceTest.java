package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.ActivityLogResponse;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.InvalidRequestException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.ActivityLogMapper;
import com.example.enterprise_issues_tracking_system.repository.ActivityLogRepository;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    private static final Long ISSUE_ID = 1L;
    private static final Long CALLER_ID = 2L;
    private static final Long PROJECT_ID = 3L;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ActivityLogMapper activityLogMapper;

    @InjectMocks
    private ActivityLogService activityLogService;

    private Project project;
    private User caller;
    private Issue issue;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(PROJECT_ID);
        caller = new User();
        caller.setId(CALLER_ID);
        issue = new Issue();
        issue.setId(ISSUE_ID);
        issue.setProject(project);
    }

    private ProjectMember buildMembership(Role role) {
        ProjectMemberId id = new ProjectMemberId();
        id.setProject(project);
        id.setUser(caller);
        ProjectMember member = new ProjectMember();
        member.setId(id);
        member.setRole(role);
        return member;
    }

    private void givenCallerRole(Role role) {
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(PROJECT_ID, CALLER_ID)).thenReturn(Optional.of(buildMembership(role)));
    }

    private void givenCallerIsNotMember() {
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(PROJECT_ID, CALLER_ID)).thenReturn(Optional.empty());
    }

    @Test
    public void getIssueActivity_whenMember_shouldRespond() {
        Pageable pageable = PageRequest.of(0, 10);
        ActivityLog log = new ActivityLog();
        ActivityLogResponse response = new ActivityLogResponse();

        when(issueRepository.findByIdAndDeletedAtIsNull(ISSUE_ID)).thenReturn(Optional.of(issue));
        givenCallerRole(Role.DEVELOPER);
        when(activityLogRepository.findAllByIssueId(ISSUE_ID, pageable)).thenReturn(new PageImpl<>(List.of(log)));
        when(activityLogMapper.toResponse(log)).thenReturn(response);

        Page<ActivityLogResponse> result = activityLogService.getIssueActivity(ISSUE_ID, pageable);

        assertEquals(List.of(response), result.getContent());
    }

    @Test
    public void getIssueActivity_whenNotMember_shouldThrowException() {
        when(issueRepository.findByIdAndDeletedAtIsNull(ISSUE_ID)).thenReturn(Optional.of(issue));
        givenCallerIsNotMember();

        assertThrows(AccessDeniedException.class, () -> activityLogService.getIssueActivity(ISSUE_ID, PageRequest.of(0, 10)));
        verify(activityLogRepository, never()).findAllByIssueId(any(), any());
    }

    @Test
    public void getIssueActivity_whenIssueNotFound_shouldThrowException() {
        when(issueRepository.findByIdAndDeletedAtIsNull(ISSUE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> activityLogService.getIssueActivity(ISSUE_ID, PageRequest.of(0, 10)));
    }

    @Test
    public void getIssueActivity_whenSortFieldNotAllowed_shouldThrowException() {
        when(issueRepository.findByIdAndDeletedAtIsNull(ISSUE_ID)).thenReturn(Optional.of(issue));
        givenCallerRole(Role.DEVELOPER);

        assertThrows(InvalidRequestException.class,
                () -> activityLogService.getIssueActivity(ISSUE_ID, PageRequest.of(0, 10, Sort.by("details"))));
        verify(activityLogRepository, never()).findAllByIssueId(any(), any());
    }

    @Test
    public void getProjectActivity_whenAdmin_shouldRespond() {
        Pageable pageable = PageRequest.of(0, 10);
        ActivityLog log = new ActivityLog();
        ActivityLogResponse response = new ActivityLogResponse();

        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        givenCallerRole(Role.ADMIN);
        when(activityLogRepository.findAll(ArgumentMatchers.<Specification<ActivityLog>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(activityLogMapper.toResponse(log)).thenReturn(response);

        Page<ActivityLogResponse> result = activityLogService.getProjectActivity(PROJECT_ID, pageable, AuditEntityType.ISSUE, null);

        assertEquals(List.of(response), result.getContent());
    }

    @Test
    public void getProjectActivity_whenProjectManager_shouldRespond() {
        Pageable pageable = PageRequest.of(0, 10);

        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        givenCallerRole(Role.PROJECT_MANAGER);
        when(activityLogRepository.findAll(ArgumentMatchers.<Specification<ActivityLog>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<ActivityLogResponse> result = activityLogService.getProjectActivity(PROJECT_ID, pageable, null, null);

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    public void getProjectActivity_whenDeveloper_shouldThrowException() {
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        givenCallerRole(Role.DEVELOPER);

        assertThrows(AccessDeniedException.class,
                () -> activityLogService.getProjectActivity(PROJECT_ID, PageRequest.of(0, 10), null, null));
        verify(activityLogRepository, never()).findAll(ArgumentMatchers.<Specification<ActivityLog>>any(), any(Pageable.class));
    }

    @Test
    public void getProjectActivity_whenNotMember_shouldThrowException() {
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        givenCallerIsNotMember();

        assertThrows(AccessDeniedException.class,
                () -> activityLogService.getProjectActivity(PROJECT_ID, PageRequest.of(0, 10), null, null));
    }

    @Test
    public void getProjectActivity_whenProjectNotFound_shouldThrowException() {
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> activityLogService.getProjectActivity(PROJECT_ID, PageRequest.of(0, 10), null, null));
    }

    @Test
    public void getProjectActivity_whenSortFieldNotAllowed_shouldThrowException() {
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        givenCallerRole(Role.ADMIN);

        assertThrows(InvalidRequestException.class,
                () -> activityLogService.getProjectActivity(PROJECT_ID, PageRequest.of(0, 10, Sort.by("actorId")), null, null));
    }
}