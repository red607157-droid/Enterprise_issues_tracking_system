package com.example.enterprise_issues_tracking_system.dto;

import java.util.List;

public record IssueByStatusResponse(long total, List<StatusCountResponse> statuses) {
}
