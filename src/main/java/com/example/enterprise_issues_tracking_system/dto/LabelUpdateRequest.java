package com.example.enterprise_issues_tracking_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LabelUpdateRequest {

    @NotBlank(message = "Label name is required")
    @Size(max = 30)
    private String name;

    @NotBlank(message = "Label color is required")
    @Size(max = 30)
    private String color;
}
