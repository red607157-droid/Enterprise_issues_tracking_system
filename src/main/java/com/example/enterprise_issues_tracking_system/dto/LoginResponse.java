package com.example.enterprise_issues_tracking_system.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
}
