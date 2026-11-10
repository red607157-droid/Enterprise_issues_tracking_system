package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.event.AuditEvent;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    static final int MAX_MESSAGE_LENGTH = 255;
    private final IssueRepository issueRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(AuditEvent event, Long actorId) {
        if (event.issueId() == null) {
            return;
        }
        NotificationType type = typeFor(event);
        if (type == null) {
            return;
        }
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(event.issueId()).orElse(null);
        if (issue == null) {
            return;
        }
        Set<Long> recipients = new HashSet<>();
        if (type != NotificationType.ISSUE_ASSIGNED) {
            addIfPresent(recipients, issue.getReporter());
        }
        addIfPresent(recipients, issue.getAssignee());
        recipients.remove(actorId);
        if (recipients.isEmpty()) {
            return;
        }
        String message = truncate(messageFor(type, issue, event));
        List<Notification> notifications = recipients.stream()
                .map(userId -> Notification.builder()
                        .projectId(event.projectId())
                        .issueId(issue.getId())
                        .message(message)
                        .type(type)
                        .userId(userId)
                        .build())
                .toList();
        notificationRepository.saveAll(notifications);
    }

    private NotificationType typeFor(AuditEvent event) {
        if (event.entityType() == AuditEntityType.ISSUE) {
            return switch (event.action()) {
                case CREATED, ASSIGNEE_CHANGED -> NotificationType.ISSUE_ASSIGNED;
                case STATUS_CHANGED -> NotificationType.ISSUE_STATUS_CHANGED;
                default -> null;
            };
        }
        if (event.entityType() == AuditEntityType.COMMENT && event.action() == AuditAction.CREATED) {
            return NotificationType.COMMENT_ADDED;
        }
        return null;
    }

    private String messageFor(NotificationType type, Issue issue, AuditEvent event) {
        return switch (type) {
            case ISSUE_ASSIGNED -> "You were assigned to \"" + issue.getTitle() + "\"";
            case ISSUE_STATUS_CHANGED -> "\"" + issue.getTitle() + "\" status changed: " + event.details();
            case COMMENT_ADDED -> "New comment on \"" + issue.getTitle() + "\"";
        };
    }

    private void addIfPresent(Set<Long> recipients, User user) {
        if (user != null) {
            recipients.add(user.getId());
        }
    }

    private String truncate(String message) {
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH);
    }
}
