package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.IssueRequest;
import com.example.enterprise_issues_tracking_system.dto.IssueResponse;
import com.example.enterprise_issues_tracking_system.dto.IssueUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.ImmutableFieldException;
import com.example.enterprise_issues_tracking_system.exception.InvalidStatusTransitionException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.IssueMapper;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final ProjectMemberRepository projectMemberRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final IssueMapper issueMapper;
    private final CurrentUserProvider currentUserProvider;

    private void checkCallerHasRole(Long projectId) {
        Long callerId = currentUserProvider.getCurrentUser().getId();
        projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to do this"));
    }

    private boolean hasRole(Long projectId, Role... roles) {
        Long callerId = currentUserProvider.getCurrentUser().getId();
        return projectMemberRepository.findMembership(projectId, callerId)
                .map(pm -> List.of(roles).contains(pm.getRole()))
                .orElse(false);
    }

    private void checkAssigneeIsMember(Long projectId, Long assigneeId) {
        if (assigneeId == null) {
            return;
        }
        projectMemberRepository.findMembership(projectId, assigneeId)
                .orElseThrow(() -> new AccessDeniedException("Assignee is not a member of the project"));
    }

    public IssueResponse createIssue(IssueRequest request) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        checkCallerHasRole(project.getId());
        checkAssigneeIsMember(project.getId(), request.getAssigneeId());
        Issue issue = issueMapper.toEntity(request);
        issue.setReporter(currentUserProvider.getCurrentUser());
        issue.setIssueStatus(IssueStatus.OPEN);
        issueRepository.save(issue);
        return issueMapper.toResponse(issue);
    }

    public IssueResponse getIssueById(Long id) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
        checkCallerHasRole(issue.getProject().getId());
        return issueMapper.toResponse(issue);
    }

    public Page<IssueResponse> getIssuesByProject(Long projectId, Pageable pageable) {
        checkCallerHasRole(projectId);
        return issueRepository.findAllByProjectIdAndDeletedAtIsNull(projectId, pageable)
                .map(issueMapper::toResponse);
    }

    public IssueResponse updateIssue(Long id, IssueUpdateRequest request) {
        Issue existingIssue = issueRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
        Long projectId = existingIssue.getProject().getId();
        Long callerId = currentUserProvider.getCurrentUser().getId();
        Long currentAssigneeId = existingIssue.getAssignee() != null ? existingIssue.getAssignee().getId() : null;

        boolean isManagerOrAdmin = hasRole(projectId, Role.PROJECT_MANAGER, Role.ADMIN);
        boolean isAssignee = callerId.equals(currentAssigneeId);
        if (!isManagerOrAdmin && !isAssignee) {
            throw new AccessDeniedException("You are not allowed to do this");
        }

        if (!Objects.equals(request.getProjectId(), projectId)) {
            throw new ImmutableFieldException("The project of an issue cannot be changed");
        }

        if (isManagerOrAdmin) {
            if (!Objects.equals(request.getAssigneeId(), currentAssigneeId)) {
                checkAssigneeIsMember(projectId, request.getAssigneeId());
            }
        } else {
            boolean restrictedFieldsUnchanged = Objects.equals(request.getAssigneeId(), currentAssigneeId)
                    && Objects.equals(request.getDueDate(), existingIssue.getDueDate());
            if (!restrictedFieldsUnchanged) {
                throw new AccessDeniedException("You are not allowed to do this");
            }
        }
        IssueStatus currentStatus = existingIssue.getIssueStatus();
        if (!currentStatus.canTransitionTo(request.getIssueStatus())) {
            throw new InvalidStatusTransitionException("Cannot move an issue from " + currentStatus + " to " + request.getIssueStatus());
        }
        Issue updatedIssue = issueMapper.updateEntity(request, existingIssue);
        issueRepository.save(updatedIssue);
        return issueMapper.toResponse(updatedIssue);
    }

    public void deleteIssue(Long id) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
        if (!hasRole(issue.getProject().getId(), Role.PROJECT_MANAGER, Role.ADMIN)) {
            throw new AccessDeniedException("You are not allowed to do this");
        }
        issue.softDelete();
        issueRepository.save(issue);
    }
}