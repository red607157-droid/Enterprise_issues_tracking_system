package com.example.enterprise_issues_tracking_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {

    private Long id;
    private Long authorId;
    private Long issueId;
    private String body;
    private LocalDateTime createdAt;
    private Long parentCommentId;
}
