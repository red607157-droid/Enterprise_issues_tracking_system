package com.example.enterprise_issues_tracking_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectRequest {

    @NotBlank(message = "key is required")
    @Size(min = 3, max = 20, message = "Project key must be at least 3 characters")
    private String key;

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 20, message = "Project name must be at least 5 characters")
    private String name;

    @Size(max = 100)
    private String description;
}
