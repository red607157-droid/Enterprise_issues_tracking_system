package com.example.enterprise_issues_tracking_system.dto;

import com.example.enterprise_issues_tracking_system.entity.IssuePriority;
import com.example.enterprise_issues_tracking_system.entity.IssueStatus;

public record IssueFilter(
        IssueStatus status,
        IssuePriority priority,
        Long assigneeId,
        Long labelId,
        String q
) {
}
