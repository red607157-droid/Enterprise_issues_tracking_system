package com.example.enterprise_issues_tracking_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectResponse {

    private Long id;
    private String key;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long ownerId;
}
