package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.CommentRequest;
import com.example.enterprise_issues_tracking_system.dto.CommentResponse;
import com.example.enterprise_issues_tracking_system.dto.CommentUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.CommentMapper;
import com.example.enterprise_issues_tracking_system.repository.CommentRepository;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final CommentRepository commentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final IssueRepository issueRepository;

    private void IsAMember(Long issueId) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long callerId = userPrincipal.getUser().getId();
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
        Long projectId = issue.getProject().getId();
        ProjectMember projectMember = projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to do this"));
    }

    private boolean IsAuthor(Long authorId) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long callerId = userPrincipal.getUser().getId();
        return Objects.equals(callerId, authorId);
    }

    public CommentResponse createComment(CommentRequest request) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(request.getIssueId())
                        .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
        IsAMember(issue.getId());
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User caller = userPrincipal.getUser();
        Comment comment = commentMapper.toEntity(request);
        comment.setAuthor(caller);
        commentRepository.save(comment);
        return commentMapper.toResponse(comment);
    }

    public CommentResponse getCommentById(Long id) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment ID not found"));
        IsAMember(comment.getIssue().getId());
        return commentMapper.toResponse(comment);
    }

    public List<CommentResponse> getCommentsByIssue(Long issueId) {
        IsAMember(issueId);
        List<Comment> allComment = commentRepository.findAllByIssueId(issueId);
        Set<Long> allParentCommentId = new HashSet<>();
        for (Comment comment : allComment) {
            if (comment.getParentComment().getId() != null) {
                allParentCommentId.add(comment.getParentComment().getId());
            }
        }
        return allComment.stream()
                .filter(comment -> comment.getDeletedAt() == null || allParentCommentId.contains(comment.getId()))
                .map(commentMapper::toResponse)
                .toList();
    }

    public CommentResponse updateComment(Long id, CommentUpdateRequest request) {
        Comment existingComment = commentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment ID not found"));
        if (!IsAuthor(existingComment.getAuthor().getId())) {
            throw new AccessDeniedException("You are not allowed to do this");
        }
        Comment newComment = commentMapper.updateEntity(request, existingComment);
        commentRepository.save(newComment);
        return commentMapper.toResponse(newComment);
    }

    public void deleteComment(Long id) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment ID not found"));
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long callerId = userPrincipal.getUser().getId();
        Long projectId = comment.getIssue().getProject().getId();
        boolean isAdminOrManager = projectMemberRepository.findMembership(projectId, callerId)
                .map(pm -> List.of(Role.ADMIN, Role.PROJECT_MANAGER).contains(pm.getRole()))
                .orElse(false);
        if (!(IsAuthor(comment.getAuthor().getId()) || isAdminOrManager)) {
            throw new AccessDeniedException("You are not allowed to do this");
        }
        comment.softDelete();
        commentRepository.save(comment);
    }
}
