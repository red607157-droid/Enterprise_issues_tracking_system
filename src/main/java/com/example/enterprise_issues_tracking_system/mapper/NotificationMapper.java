package com.example.enterprise_issues_tracking_system.mapper;

import com.example.enterprise_issues_tracking_system.dto.NotificationResponse;
import com.example.enterprise_issues_tracking_system.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {

        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setIssueId(notification.getIssueId());
        response.setType(notification.getType());
        response.setMessage(notification.getMessage());
        response.setCreatedAt(notification.getCreatedAt());
        response.setProjectId(notification.getProjectId());
        response.setRead(notification.isRead());
        return response;
    }
}
