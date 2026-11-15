package com.example.enterprise_issues_tracking_system.dto;

import com.example.enterprise_issues_tracking_system.entity.IssueStatus;

public record StatusCountResponse(IssueStatus status, long count) {
}
