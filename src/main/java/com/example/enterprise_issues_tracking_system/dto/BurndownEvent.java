package com.example.enterprise_issues_tracking_system.dto;

import com.example.enterprise_issues_tracking_system.entity.AuditAction;

import java.time.LocalDateTime;

public record BurndownEvent(Long issueId, AuditAction action, String details, LocalDateTime createdAt) {
}
