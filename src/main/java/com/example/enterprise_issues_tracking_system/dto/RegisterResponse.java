package com.example.enterprise_issues_tracking_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegisterResponse {
    private Long id;
    private String email;
    private String fullName;
    private LocalDateTime createdAt;
}
