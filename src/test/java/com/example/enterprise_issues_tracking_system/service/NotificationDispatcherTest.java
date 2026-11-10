package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.event.AuditEvent;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    private static final Long ISSUE_ID = 1L;
    private static final Long PROJECT_ID = 2L;
    private static final Long REPORTER_ID = 10L;
    private static final Long ASSIGNEE_ID = 20L;
    private static final Long THIRD_PARTY_ID = 99L;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Captor
    private ArgumentCaptor<List<Notification>> savedCaptor;

    @InjectMocks
    private NotificationDispatcher notificationDispatcher;

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Issue givenIssue(Long reporterId, Long assigneeId) {
        Issue issue = new Issue();
        issue.setId(ISSUE_ID);
        issue.setTitle("Login bug");
        issue.setReporter(buildUser(reporterId));
        if (assigneeId != null) {
            issue.setAssignee(buildUser(assigneeId));
        }
        when(issueRepository.findByIdAndDeletedAtIsNull(ISSUE_ID)).thenReturn(Optional.of(issue));
        return issue;
    }

    private AuditEvent issueEvent(AuditAction action, String details) {
        return new AuditEvent(AuditEntityType.ISSUE, ISSUE_ID, PROJECT_ID, ISSUE_ID, action, details);
    }

    private AuditEvent commentEvent(AuditAction action) {
        return new AuditEvent(AuditEntityType.COMMENT, 5L, PROJECT_ID, ISSUE_ID, action, null);
    }

    private List<Notification> savedNotifications() {
        verify(notificationRepository).saveAll(savedCaptor.capture());
        return savedCaptor.getValue();
    }

    @Test
    public void dispatch_whenIssueCreatedWithAssignee_shouldNotifyTheAssignee() {
        givenIssue(REPORTER_ID, ASSIGNEE_ID);

        notificationDispatcher.dispatch(issueEvent(AuditAction.CREATED, "Login bug"), REPORTER_ID);

        List<Notification> saved = savedNotifications();
        assertEquals(1, saved.size());
        Notification notification = saved.getFirst();
        assertEquals(ASSIGNEE_ID, notification.getUserId());
        assertEquals(NotificationType.ISSUE_ASSIGNED, notification.getType());
        assertTrue(notification.getMessage().contains("Login bug"));
        assertEquals(PROJECT_ID, notification.getProjectId());
        assertEquals(ISSUE_ID, notification.getIssueId());
        assertFalse(notification.isRead());
    }

    @Test
    public void dispatch_whenIssueCreatedWithoutAssignee_shouldNotifyNobody() {
        givenIssue(REPORTER_ID, null);

        notificationDispatcher.dispatch(issueEvent(AuditAction.CREATED, "Login bug"), REPORTER_ID);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    public void dispatch_whenUserAssignsThemselves_shouldNotifyNobody() {
        givenIssue(REPORTER_ID, ASSIGNEE_ID);

        notificationDispatcher.dispatch(issueEvent(AuditAction.CREATED, "Login bug"), ASSIGNEE_ID);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    public void dispatch_whenAssigneeChanged_shouldNotifyTheNewAssigneeOnly() {
        givenIssue(REPORTER_ID, ASSIGNEE_ID);

        notificationDispatcher.dispatch(issueEvent(AuditAction.ASSIGNEE_CHANGED, "assignee 5 -> 20"), THIRD_PARTY_ID);

        List<Notification> saved = savedNotifications();
        assertEquals(1, saved.size());
        assertEquals(ASSIGNEE_ID, saved.getFirst().getUserId());
        assertEquals(NotificationType.ISSUE_ASSIGNED, saved.getFirst().getType());
    }

    @Test
    public void dispatch_whenAssigneeRemoved_shouldNotifyNobody() {
        givenIssue(REPORTER_ID, null);

        notificationDispatcher.dispatch(issueEvent(AuditAction.ASSIGNEE_CHANGED, "assignee 5 -> null"), THIRD_PARTY_ID);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    public void dispatch_whenStatusChanged_shouldNotifyReporterAndAssignee() {
        givenIssue(REPORTER_ID, ASSIGNEE_ID);

        notificationDispatcher.dispatch(issueEvent(AuditAction.STATUS_CHANGED, "OPEN -> IN_PROGRESS"), THIRD_PARTY_ID);

        List<Notification> saved = savedNotifications();
        assertEquals(List.of(REPORTER_ID, ASSIGNEE_ID), saved.stream().map(Notification::getUserId).toList());
        assertTrue(saved.stream().allMatch(n -> n.getType() == NotificationType.ISSUE_STATUS_CHANGED));
        assertTrue(saved.getFirst().getMessage().contains("OPEN -> IN_PROGRESS"));
    }

    @Test
    public void dispatch_whenAssigneeChangesStatus_shouldNotifyOnlyTheReporter() {
        givenIssue(REPORTER_ID, ASSIGNEE_ID);

        notificationDispatcher.dispatch(issueEvent(AuditAction.STATUS_CHANGED, "OPEN -> IN_PROGRESS"), ASSIGNEE_ID);

        List<Notification> saved = savedNotifications();
        assertEquals(1, saved.size());
        assertEquals(REPORTER_ID, saved.get(0).getUserId());
    }

    @Test
    public void dispatch_whenReporterIsAlsoAssignee_shouldNotifyThemOnce() {
        givenIssue(REPORTER_ID, REPORTER_ID);

        notificationDispatcher.dispatch(issueEvent(AuditAction.STATUS_CHANGED, "OPEN -> IN_PROGRESS"), THIRD_PARTY_ID);

        assertEquals(1, savedNotifications().size());
    }

    @Test
    public void dispatch_whenCommentAdded_shouldNotifyReporterAndAssigneeExceptTheAuthor() {
        givenIssue(REPORTER_ID, ASSIGNEE_ID);

        notificationDispatcher.dispatch(commentEvent(AuditAction.CREATED), REPORTER_ID);

        List<Notification> saved = savedNotifications();
        assertEquals(1, saved.size());
        assertEquals(ASSIGNEE_ID, saved.getFirst().getUserId());
        assertEquals(NotificationType.COMMENT_ADDED, saved.get(0).getType());
    }

    @Test
    public void dispatch_whenActionDoesNotNotify_shouldNotEvenLoadTheIssue() {
        notificationDispatcher.dispatch(issueEvent(AuditAction.LABEL_ATTACHED, "label 4"), THIRD_PARTY_ID);
        notificationDispatcher.dispatch(issueEvent(AuditAction.UPDATED, "changed: title"), THIRD_PARTY_ID);
        notificationDispatcher.dispatch(issueEvent(AuditAction.DELETED, "Login bug"), THIRD_PARTY_ID);
        notificationDispatcher.dispatch(commentEvent(AuditAction.DELETED), THIRD_PARTY_ID);

        verify(issueRepository, never()).findByIdAndDeletedAtIsNull(any());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    public void dispatch_whenEventHasNoIssue_shouldDoNothing() {
        AuditEvent projectEvent = new AuditEvent(AuditEntityType.PROJECT, PROJECT_ID, PROJECT_ID, null, AuditAction.CREATED, "key ABC");

        notificationDispatcher.dispatch(projectEvent, THIRD_PARTY_ID);

        verify(issueRepository, never()).findByIdAndDeletedAtIsNull(any());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    public void dispatch_whenIssueNoLongerExists_shouldDoNothing() {
        when(issueRepository.findByIdAndDeletedAtIsNull(ISSUE_ID)).thenReturn(Optional.empty());

        notificationDispatcher.dispatch(commentEvent(AuditAction.CREATED), THIRD_PARTY_ID);

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    public void dispatch_whenTitleIsVeryLong_shouldTruncateTheMessage() {
        Issue issue = givenIssue(REPORTER_ID, ASSIGNEE_ID);
        issue.setTitle("x".repeat(400));

        notificationDispatcher.dispatch(issueEvent(AuditAction.CREATED, null), REPORTER_ID);

        assertEquals(NotificationDispatcher.MAX_MESSAGE_LENGTH, savedNotifications().getFirst().getMessage().length());
    }
}