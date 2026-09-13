package com.example.enterprise_issues_tracking_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LabelResponse {

    private Long id;
    private String name;
    private String color;
    private Long projectId;
    private LocalDateTime createdAt;
}
