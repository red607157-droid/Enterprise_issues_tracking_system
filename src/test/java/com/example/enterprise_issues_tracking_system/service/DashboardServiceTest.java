package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.*;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.InvalidRequestException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.repository.ActivityLogRepository;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Long PROJECT_ID = 3L;
    private static final Long CALLER_ID = 2L;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private DashboardService dashboardService;

    private Project project;
    private User caller;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(PROJECT_ID);
        caller = new User();
        caller.setId(CALLER_ID);
    }

    private void givenCallerIsMember() {
        ProjectMemberId id = new ProjectMemberId();
        id.setProject(project);
        id.setUser(caller);
        ProjectMember member = new ProjectMember();
        member.setId(id);
        member.setRole(Role.DEVELOPER);
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(PROJECT_ID, CALLER_ID)).thenReturn(Optional.of(member));
    }

    private void givenCallerIsNotMember() {
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(PROJECT_ID, CALLER_ID)).thenReturn(Optional.empty());
    }

    @Test
    public void getIssuesByStatus_shouldListEveryStatusIncludingZeros() {
        givenCallerIsMember();
        when(issueRepository.countByStatus(PROJECT_ID)).thenReturn(List.of(
                new Object[]{IssueStatus.OPEN, 3L},
                new Object[]{IssueStatus.DONE, 1L}));

        IssueByStatusResponse result = dashboardService.getIssuesByProject(PROJECT_ID);

        assertEquals(4, result.total());
        assertEquals(List.of(
                new StatusCountResponse(IssueStatus.OPEN, 3),
                new StatusCountResponse(IssueStatus.IN_PROGRESS, 0),
                new StatusCountResponse(IssueStatus.IN_REVIEW, 0),
                new StatusCountResponse(IssueStatus.DONE, 1)), result.statuses());
    }

    @Test
    public void getIssuesByStatus_whenProjectHasNoIssues_shouldReturnAllZeros() {
        givenCallerIsMember();
        when(issueRepository.countByStatus(PROJECT_ID)).thenReturn(List.of());

        IssueByStatusResponse result = dashboardService.getIssuesByProject(PROJECT_ID);

        assertEquals(0, result.total());
        assertEquals(4, result.statuses().size());
        assertTrue(result.statuses().stream().allMatch(s -> s.count() == 0));
    }

    @Test
    public void getIssuesByStatus_whenNotMember_shouldThrowException() {
        givenCallerIsNotMember();

        assertThrows(AccessDeniedException.class, () -> dashboardService.getIssuesByProject(PROJECT_ID));
        verify(issueRepository, never()).countByStatus(any());
    }

    @Test
    public void getIssuesByStatus_whenProjectNotFound_shouldThrowException() {
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> dashboardService.getIssuesByProject(PROJECT_ID));
    }

    @Test
    public void getWorkloadByAssignee_shouldMapSortAndLabelTheUnassignedRow() {
        givenCallerIsMember();
        when(issueRepository.countByAssignee(PROJECT_ID, IssueStatus.DONE)).thenReturn(List.of(
                new Object[]{5L, "Ana", 3L, 1L},
                new Object[]{null, null, 2L, 0L},
                new Object[]{7L, "Bao", 4L, 0L},
                new Object[]{8L, "Cuong", 1L, null}));   

        List<AssigneeWorkloadResponse> result = dashboardService.getWorkloadByAssignee(PROJECT_ID);

        assertEquals(List.of(
                new AssigneeWorkloadResponse(7L, "Bao", 4, 4),
                new AssigneeWorkloadResponse(5L, "Ana", 3, 2),
                new AssigneeWorkloadResponse(null, "Unassigned", 2, 2),
                new AssigneeWorkloadResponse(8L, "Cuong", 1, 1)), result);
    }

    @Test
    public void getWorkloadByAssignee_whenNotMember_shouldThrowException() {
        givenCallerIsNotMember();

        assertThrows(AccessDeniedException.class, () -> dashboardService.getWorkloadByAssignee(PROJECT_ID));
        verify(issueRepository, never()).countByAssignee(any(), any());
    }
    
    @Test
    public void getBurndown_withExplicitRange_shouldReplayTheAuditEvents() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 3);
        BurndownEvent createdOnDay2 =
                new BurndownEvent(1L, AuditAction.CREATED, "title", LocalDateTime.of(2026, 11, 2, 10, 0));
        givenCallerIsMember();
        when(activityLogRepository.findBurnDownEvents(
                PROJECT_ID,
                AuditEntityType.ISSUE,
                List.of(AuditAction.CREATED, AuditAction.STATUS_CHANGED, AuditAction.DELETED),
                LocalDateTime.of(2026, 11, 4, 0, 0)))
                .thenReturn(List.of(createdOnDay2));

        BurndownResponse result = dashboardService.getBurnDown(PROJECT_ID, from, to);

        assertEquals(from, result.from());
        assertEquals(to, result.to());
        assertEquals(List.of(
                new BurndownPointResponse(from, 0, 0, 0),
                new BurndownPointResponse(LocalDate.of(2026, 11, 2), 1, 1, 0),
                new BurndownPointResponse(to, 1, 0, 0)), result.points());
    }

    @Test
    public void getBurndown_withoutDates_shouldDefaultToTheLastFourteenDays() {
        givenCallerIsMember();
        when(activityLogRepository.findBurnDownEvents(eq(PROJECT_ID), eq(AuditEntityType.ISSUE), any(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        BurndownResponse result = dashboardService.getBurnDown(PROJECT_ID, null, null);

        assertEquals(LocalDate.now(), result.to());
        assertEquals(14, result.points().size());
        assertEquals(LocalDate.now().minusDays(13), result.from());
    }

    @Test
    public void getBurndown_whenRangeIsExactlyTheMaximum_shouldBeAllowed() {
        givenCallerIsMember();
        when(activityLogRepository.findBurnDownEvents(eq(PROJECT_ID), eq(AuditEntityType.ISSUE), any(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        BurndownResponse result = dashboardService.getBurnDown(
                PROJECT_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 31));

        assertEquals(DashboardService.MAX_BURNDOWN_DAYS, result.points().size());
    }

    @Test
    public void getBurndown_whenRangeIsTooLong_shouldThrowException() {
        givenCallerIsMember();

        assertThrows(InvalidRequestException.class,
                () -> dashboardService.getBurnDown(PROJECT_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 1)));
        verify(activityLogRepository, never()).findBurnDownEvents(any(), any(), any(), any());
    }

    @Test
    public void getBurndown_whenFromIsAfterTo_shouldThrowException() {
        givenCallerIsMember();

        assertThrows(InvalidRequestException.class,
                () -> dashboardService.getBurnDown(PROJECT_ID, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1)));
        verify(activityLogRepository, never()).findBurnDownEvents(any(), any(), any(), any());
    }

    @Test
    public void getBurndown_whenNotMember_shouldThrowExceptionBeforeValidatingTheRange() {
        givenCallerIsNotMember();

        assertThrows(AccessDeniedException.class,
                () -> dashboardService.getBurnDown(PROJECT_ID, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1)));
        verify(activityLogRepository, never()).findBurnDownEvents(any(), any(), any(), any());
    }
}