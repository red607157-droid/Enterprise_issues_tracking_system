package com.example.enterprise_issues_tracking_system.mapper;

import com.example.enterprise_issues_tracking_system.dto.ActivityLogResponse;
import com.example.enterprise_issues_tracking_system.entity.ActivityLog;
import org.springframework.stereotype.Component;

@Component
public class ActivityLogMapper {

    public ActivityLogResponse toResponse(ActivityLog log) {
        ActivityLogResponse response = new ActivityLogResponse();
        response.setId(log.getId());
        response.setEntityType(log.getEntityType());
        response.setEntityId(log.getEntityId());
        response.setProjectId(log.getProjectId());
        response.setIssueId(log.getIssueId());
        response.setAction(log.getAction());
        response.setActorId(log.getActorId());
        response.setDetails(log.getDetails());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}