package com.example.enterprise_issues_tracking_system.dto;

import com.example.enterprise_issues_tracking_system.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Role is required")
    private Role role;
}
