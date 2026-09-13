package com.example.enterprise_issues_tracking_system.dto;

import com.example.enterprise_issues_tracking_system.entity.IssuePriority;
import com.example.enterprise_issues_tracking_system.entity.IssueStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IssueResponse {

    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private String type;
    private IssueStatus issueStatus;
    private IssuePriority issuePriority;
    private Long assigneeId;
    private Long reporterId;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
