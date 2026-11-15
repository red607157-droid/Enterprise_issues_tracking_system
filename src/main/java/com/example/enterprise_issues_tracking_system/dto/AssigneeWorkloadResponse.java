package com.example.enterprise_issues_tracking_system.dto;

public record AssigneeWorkloadResponse(Long assigneeId, String assigneeName, long total, long open) {
}
