package com.example.enterprise_issues_tracking_system.event;

import com.example.enterprise_issues_tracking_system.entity.AuditAction;
import com.example.enterprise_issues_tracking_system.entity.AuditEntityType;

public record AuditEvent(
        AuditEntityType entityType,
        Long entityId,
        Long projectId,
        Long issueId,
        AuditAction action,
        String details) {
}