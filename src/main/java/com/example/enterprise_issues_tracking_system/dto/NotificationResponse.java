package com.example.enterprise_issues_tracking_system.dto;

import com.example.enterprise_issues_tracking_system.entity.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String message;
    private Long projectId;
    private Long issueId;
    private boolean read;
    private LocalDateTime createdAt;
}