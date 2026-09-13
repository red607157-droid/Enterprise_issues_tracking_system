package com.example.enterprise_issues_tracking_system.controller;

import com.example.enterprise_issues_tracking_system.dto.CommentRequest;
import com.example.enterprise_issues_tracking_system.dto.CommentResponse;
import com.example.enterprise_issues_tracking_system.dto.CommentUpdateRequest;
import com.example.enterprise_issues_tracking_system.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/comments")
    public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(request));
    }

    @GetMapping("/comments/{id}")
    public ResponseEntity<CommentResponse> getCommentById(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.getCommentById(id));
    }

    @GetMapping("/issues/{issueId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsByIssue(@PathVariable Long issueId) {
        return ResponseEntity.ok(commentService.getCommentsByIssue(issueId));
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Long id, @Valid @RequestBody CommentUpdateRequest request) {
        return ResponseEntity.ok(commentService.updateComment(id, request));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
