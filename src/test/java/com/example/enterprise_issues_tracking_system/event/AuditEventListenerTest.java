package com.example.enterprise_issues_tracking_system.event;

import com.example.enterprise_issues_tracking_system.entity.ActivityLog;
import com.example.enterprise_issues_tracking_system.entity.AuditAction;
import com.example.enterprise_issues_tracking_system.entity.AuditEntityType;
import com.example.enterprise_issues_tracking_system.entity.User;
import com.example.enterprise_issues_tracking_system.repository.ActivityLogRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private AuditEventListener auditEventListener;

    private ActivityLog captureSavedLog() {
        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    public void onAuditEvent_shouldSaveRowWithActorTakenFromTheAuthenticatedUser() {
        User caller = new User();
        caller.setId(7L);
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        AuditEvent event = new AuditEvent(AuditEntityType.ISSUE, 1L, 2L, 1L, AuditAction.STATUS_CHANGED, "OPEN -> DONE");

        auditEventListener.onAuditEvent(event);

        ActivityLog saved = captureSavedLog();
        assertEquals(AuditEntityType.ISSUE, saved.getEntityType());
        assertEquals(1L, saved.getEntityId());
        assertEquals(2L, saved.getProjectId());
        assertEquals(1L, saved.getIssueId());
        assertEquals(AuditAction.STATUS_CHANGED, saved.getAction());
        assertEquals(7L, saved.getActorId());
        assertEquals("OPEN -> DONE", saved.getDetails());
    }

    @Test
    public void onAuditEvent_whenDetailsTooLong_shouldTruncateToColumnSize() {
        User caller = new User();
        caller.setId(7L);
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        String longDetails = "x".repeat(AuditEventListener.MAX_DETAILS_LENGTH + 100);
        AuditEvent event = new AuditEvent(AuditEntityType.ATTACHMENT, 3L, 2L, 1L, AuditAction.CREATED, longDetails);

        auditEventListener.onAuditEvent(event);

        assertEquals(AuditEventListener.MAX_DETAILS_LENGTH, captureSavedLog().getDetails().length());
    }

    @Test
    public void onAuditEvent_whenNoDetails_shouldSaveNullDetailsAndNoIssue() {
        User caller = new User();
        caller.setId(7L);
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        AuditEvent event = new AuditEvent(AuditEntityType.PROJECT, 2L, 2L, null, AuditAction.DELETED, null);

        auditEventListener.onAuditEvent(event);

        ActivityLog saved = captureSavedLog();
        assertNull(saved.getDetails());
        assertNull(saved.getIssueId());
    }
}