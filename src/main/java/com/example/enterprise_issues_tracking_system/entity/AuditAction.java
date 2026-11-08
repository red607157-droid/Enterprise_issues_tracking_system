package com.example.enterprise_issues_tracking_system.entity;

public enum AuditAction {
    CREATED,
    UPDATED,
    DELETED,
    STATUS_CHANGED,
    ASSIGNEE_CHANGED,
    ROLE_CHANGED,
    LABEL_ATTACHED,
    LABEL_DETACHED
}