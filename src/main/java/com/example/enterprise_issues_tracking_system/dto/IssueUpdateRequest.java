package com.example.enterprise_issues_tracking_system.dto;

import com.example.enterprise_issues_tracking_system.entity.IssuePriority;
import com.example.enterprise_issues_tracking_system.entity.IssueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IssueUpdateRequest {

    @NotBlank(message = "Issue title is required")
    @Size(max = 100)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotBlank(message = "Issue type is required")
    @Size(max = 20)
    private String type;

    @NotNull(message = "Issue status is required")
    private IssueStatus issueStatus;

    @NotNull(message = "Issue priority is required")
    private IssuePriority issuePriority;

    private Long assigneeId;

    @NotNull(message = "Issue detected date is required")
    private LocalDateTime dueDate;

    @NotNull(message = "Project Id is required")
    private Long projectId;
}
