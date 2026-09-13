package com.example.enterprise_issues_tracking_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentUpdateRequest {

    @NotBlank(message = "Body is required")
    @Size(max = 1000)
    private String body;
}
