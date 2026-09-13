package com.example.enterprise_issues_tracking_system.dto;

import com.example.enterprise_issues_tracking_system.entity.IssuePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IssueRequest {

    @NotNull(message = "Project Id is required")
    private Long projectId;

    @NotBlank(message = "Issue title is required")
    @Size(max = 100)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotBlank(message = "Issue type is required")
    @Size(max = 20)
    private String type;

    @NotNull(message = "Issue priority is required")
    private IssuePriority issuePriority;

    private Long assigneeId;

    @NotNull(message = "Issue reporter is required")
    private Long reporterId;

    @NotNull(message = "Issue detected date is required")
    private LocalDateTime dueDate;
}
