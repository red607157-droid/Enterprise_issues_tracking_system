package com.example.enterprise_issues_tracking_system.event;

import com.example.enterprise_issues_tracking_system.entity.AuditAction;
import com.example.enterprise_issues_tracking_system.entity.AuditEntityType;
import com.example.enterprise_issues_tracking_system.entity.User;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import com.example.enterprise_issues_tracking_system.service.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    private final AuditEvent event =
            new AuditEvent(AuditEntityType.COMMENT, 5L, 2L, 1L, AuditAction.CREATED, null);

    private void givenCaller() {
        User caller = new User();
        caller.setId(7L);
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
    }

    @Test
    public void onAuditEvent_shouldPassTheAuthenticatedUserAsTheActor() {
        givenCaller();

        notificationEventListener.onAuditEvent(event);

        verify(notificationDispatcher).dispatch(event, 7L);
    }

    @Test
    public void onAuditEvent_whenDispatchFails_shouldNotPropagateTheError() {
        givenCaller();
        doThrow(new RuntimeException("database down")).when(notificationDispatcher).dispatch(event, 7L);

        assertDoesNotThrow(() -> notificationEventListener.onAuditEvent(event));
    }
}