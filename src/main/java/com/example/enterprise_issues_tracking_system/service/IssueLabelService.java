package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.LabelResponse;
import com.example.enterprise_issues_tracking_system.entity.Issue;
import com.example.enterprise_issues_tracking_system.entity.IssueLabel;
import com.example.enterprise_issues_tracking_system.entity.IssueLabelId;
import com.example.enterprise_issues_tracking_system.entity.Label;
import com.example.enterprise_issues_tracking_system.entity.Role;
import com.example.enterprise_issues_tracking_system.exception.DuplicateResourceException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.LabelMapper;
import com.example.enterprise_issues_tracking_system.repository.IssueLabelRepository;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.LabelRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueLabelService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final LabelRepository labelRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final LabelMapper labelMapper;

    public void addLabel(Long issueId, Long labelId) {
        Issue issue = findActiveIssue(issueId);
        Long projectId = issue.getProject().getId();
        checkProjectIsActive(projectId);
        checkCallerCanEdit(projectId, issue);
        Label label = labelRepository.findByIdAndDeletedAtIsNull(labelId)
                .filter(found -> projectId.equals(found.getProject().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Label ID not found in this project"));
        if (issueLabelRepository.findLink(issueId, labelId).isPresent()) {
            throw new DuplicateResourceException("Label is already attached to this issue");
        }
        IssueLabelId linkId = new IssueLabelId();
        linkId.setIssue(issue);
        linkId.setLabel(label);
        IssueLabel link = new IssueLabel();
        link.setId(linkId);
        issueLabelRepository.save(link);
    }

    public void removeLabel(Long issueId, Long labelId) {
        Issue issue = findActiveIssue(issueId);
        Long projectId = issue.getProject().getId();
        checkProjectIsActive(projectId);
        checkCallerCanEdit(projectId, issue);
        IssueLabel link = issueLabelRepository.findLink(issueId, labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label is not attached to this issue"));
        issueLabelRepository.delete(link);
    }

    public List<LabelResponse> getLabelsByIssue(Long issueId) {
        Issue issue = findActiveIssue(issueId);
        Long projectId = issue.getProject().getId();
        checkProjectIsActive(projectId);
        Long callerId = currentUserProvider.getCurrentUser().getId();
        projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to do this"));
        return issueLabelRepository.findActiveLabelsByIssueId(issueId).stream()
                .map(labelMapper::toResponse)
                .toList();
    }

    private Issue findActiveIssue(Long issueId) {
        return issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
    }

    private void checkProjectIsActive(Long projectId) {
        projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
    }

    private void checkCallerCanEdit(Long projectId, Issue issue) {
        Long callerId = currentUserProvider.getCurrentUser().getId();
        boolean isManagerOrAdmin = projectMemberRepository.findMembership(projectId, callerId)
                .map(member -> List.of(Role.ADMIN, Role.PROJECT_MANAGER).contains(member.getRole()))
                .orElse(false);
        boolean isAssignee = issue.getAssignee() != null && callerId.equals(issue.getAssignee().getId());
        if (!isManagerOrAdmin && !isAssignee) {
            throw new AccessDeniedException("You are not allowed to do this");
        }
    }
}