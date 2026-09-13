package com.example.enterprise_issues_tracking_system.mapper;

import com.example.enterprise_issues_tracking_system.dto.CommentRequest;
import com.example.enterprise_issues_tracking_system.dto.CommentResponse;
import com.example.enterprise_issues_tracking_system.dto.CommentUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.Comment;
import com.example.enterprise_issues_tracking_system.entity.Issue;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CommentMapper {

    public Comment toEntity(CommentRequest request) {

        Issue issue = new Issue();
        issue.setId(request.getIssueId());

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = new Comment();
            parentComment.setId(request.getParentCommentId());
        }

        Comment comment = new Comment();
        comment.setBody(request.getBody());
        comment.setIssue(issue);
        comment.setParentComment(parentComment);
        comment.setCreatedAt(LocalDateTime.now());

        return comment;
    }

    public CommentResponse toResponse(Comment comment) {

        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setAuthorId(comment.getAuthor().getId());
        response.setIssueId(comment.getIssue().getId());
        response.setBody(comment.getDeletedAt() == null ? comment.getBody() : "[deleted]");
        response.setCreatedAt(comment.getCreatedAt());
        response.setParentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null);

        return response;
    }

    public Comment updateEntity(CommentUpdateRequest request, Comment existingComment) {

        existingComment.setBody(request.getBody());

        return existingComment;
    }
}
