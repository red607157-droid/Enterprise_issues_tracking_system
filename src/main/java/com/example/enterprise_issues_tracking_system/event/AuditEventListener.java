package com.example.enterprise_issues_tracking_system.event;

import com.example.enterprise_issues_tracking_system.entity.ActivityLog;
import com.example.enterprise_issues_tracking_system.repository.ActivityLogRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    static final int MAX_DETAILS_LENGTH  = 500;

    private final ActivityLogRepository activityLogRepository;
    private final CurrentUserProvider currentUserProvider;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onAuditEvent(AuditEvent event) {
        ActivityLog log = ActivityLog.builder()
                .entityType(event.entityType())
                .entityId(event.entityId())
                .projectId(event.projectId())
                .issueId(event.issueId())
                .action(event.action())
                .actorId(currentUserProvider.getCurrentUser().getId())
                .details(truncate(event.details()))
                .build();
        activityLogRepository.save(log);
    }

    private String truncate(String details) {
        if (details == null || details.length() <= MAX_DETAILS_LENGTH) {
            return details;
        }
        return details.substring(0, MAX_DETAILS_LENGTH);
    }
}
