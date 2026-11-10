package com.example.enterprise_issues_tracking_system.event;

import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import com.example.enterprise_issues_tracking_system.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationDispatcher notificationDispatcher;
    private final CurrentUserProvider currentUserProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAuditEvent(AuditEvent event) {
        try {
            Long actorId = currentUserProvider.getCurrentUser().getId();
            notificationDispatcher.dispatch(event, actorId);
        } catch (RuntimeException exception) {
            log.error("Could not create notifications for {} {} on {} {}",
                    event.action(), event.entityType(), event.entityId(), exception.getMessage(), exception);
        }
    }
}
