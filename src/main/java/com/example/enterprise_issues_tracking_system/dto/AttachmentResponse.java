package com.example.enterprise_issues_tracking_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentResponse {

    private Long id;
    private Long issueId;
    private Long uploadedBy;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;
}
