package com.example.enterprise_issues_tracking_system.dto;

import com.example.enterprise_issues_tracking_system.entity.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectMemberResponse {

    private Long projectId;
    private Long userId;
    private Role role;
    private LocalDateTime createdAt;
}
