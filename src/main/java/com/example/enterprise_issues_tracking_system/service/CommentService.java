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
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import com.example.enterprise_issues_tracking_system.util.PageableValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

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
    private final CurrentUserProvider currentUserProvider;
    private static final Set<String> SORTED_FIELDS = Set.of("id", "createdAt");

    private void IsAMember(Long issueId) {
        Long callerId = currentUserProvider.getCurrentUser().getId();
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
        Long projectId = issue.getProject().getId();
        ProjectMember projectMember = projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to do this"));
    }

    private boolean IsAuthor(Long authorId) {
        Long callerId = currentUserProvider.getCurrentUser().getId();
        return Objects.equals(callerId, authorId);
    }

    public CommentResponse createComment(CommentRequest request) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(request.getIssueId())
                        .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
        IsAMember(issue.getId());
        User caller = currentUserProvider.getCurrentUser();
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

    public Page<CommentResponse> getCommentsByIssue(Long issueId, Pageable pageable) {
        IsAMember(issueId);
        PageableValidator.requireSortableBy(pageable, SORTED_FIELDS);
        return commentRepository.findVisibleByIssueId(issueId, pageable)
                .map(commentMapper::toResponse);
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
        Long callerId = currentUserProvider.getCurrentUser().getId();
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
