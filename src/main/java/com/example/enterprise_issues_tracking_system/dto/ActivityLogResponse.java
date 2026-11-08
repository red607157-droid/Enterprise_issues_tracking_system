package com.example.enterprise_issues_tracking_system.dto;

import com.example.enterprise_issues_tracking_system.entity.AuditAction;
import com.example.enterprise_issues_tracking_system.entity.AuditEntityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityLogResponse {

    private Long id;
    private AuditEntityType entityType;
    private Long entityId;
    private Long projectId;
    private Long issueId;
    private AuditAction action;
    private Long actorId;
    private String details;
    private LocalDateTime createdAt;
}