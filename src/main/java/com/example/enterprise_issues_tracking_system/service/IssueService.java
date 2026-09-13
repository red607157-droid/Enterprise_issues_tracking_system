package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.IssueRequest;
import com.example.enterprise_issues_tracking_system.dto.IssueResponse;
import com.example.enterprise_issues_tracking_system.dto.IssueUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.IssueMapper;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private void checkCallerHasRole(Long projectId) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long callerId = userPrincipal.getUser().getId();
        ProjectMember projectMember = projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to do this"));
    }

    private boolean hasRole(Long projectId, Role... roles) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long callerId = userPrincipal.getUser().getId();
        return projectMemberRepository.findMembership(projectId, callerId)
                .map(pm -> List.of(roles).contains(pm.getRole()))
                .orElse(false);
    }

    public IssueResponse createIssue(IssueRequest request) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        checkCallerHasRole(project.getId());
        if (request.getAssigneeId() != null) {
            ProjectMember projectMember = projectMemberRepository.findMembership(request.getProjectId(), request.getAssigneeId())
                    .orElseThrow(() -> new AccessDeniedException("Assignee is not a member of the project"));
        }
        Issue issue = issueMapper.toEntity(request);
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
                .map(issueMapper :: toResponse);
    }

    public IssueResponse updateIssue(Long id, IssueUpdateRequest request) {
        Issue existingIssue = issueRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long callerId = userPrincipal.getUser().getId();
        boolean restrictedFieldsUnchanged = Objects.equals(request.getAssigneeId(), existingIssue.getAssignee() != null ? existingIssue.getAssignee().getId() : null)
                && Objects.equals(request.getReporterId(), existingIssue.getReporter().getId())
                && Objects.equals(request.getProjectId(), existingIssue.getProject().getId())
                && Objects.equals(request.getDueDate(), existingIssue.getDueDate());
        if (hasRole(existingIssue.getProject().getId(), Role.PROJECT_MANAGER, Role.ADMIN)) {
            Issue newIssue = issueMapper.updateEntity(request, existingIssue);
            issueRepository.save(newIssue);
            return issueMapper.toResponse(newIssue);
        } else if (existingIssue.getAssignee() != null && existingIssue.getAssignee().getId().equals(callerId)) {
            if (restrictedFieldsUnchanged) {
                Issue newIssue = issueMapper.updateEntity(request, existingIssue);
                issueRepository.save(newIssue);
                return issueMapper.toResponse(newIssue);
            } else {
                throw new AccessDeniedException("You are not allowed to do this");
            }
        } else {
            throw new AccessDeniedException("You are not allowed to do this");
        }
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
