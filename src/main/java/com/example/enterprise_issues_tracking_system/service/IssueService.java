package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.IssueFilter;
import com.example.enterprise_issues_tracking_system.dto.IssueRequest;
import com.example.enterprise_issues_tracking_system.dto.IssueResponse;
import com.example.enterprise_issues_tracking_system.dto.IssueUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.event.AuditEvent;
import com.example.enterprise_issues_tracking_system.exception.ImmutableFieldException;
import com.example.enterprise_issues_tracking_system.exception.InvalidStatusTransitionException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.IssueMapper;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.IssueSpecifications;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import com.example.enterprise_issues_tracking_system.util.PageableValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;


@Service
@RequiredArgsConstructor
@Transactional
public class IssueService {

    private final ProjectMemberRepository projectMemberRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final IssueMapper issueMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "title", "type", "issuePriority", "issueStatus", "dueDate", "createdAt", "updatedAt");

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

    private void publish(AuditAction action, Long projectId, Long issueId, String details) {
        eventPublisher.publishEvent(new AuditEvent(AuditEntityType.ISSUE, issueId, projectId, issueId, action, details));
    }

    private List<String> changedFields(Issue issue, IssueUpdateRequest request) {
        List<String> changed = new ArrayList<>();
        if (!Objects.equals(issue.getTitle(), request.getTitle())) changed.add("title");
        if (!Objects.equals(issue.getDescription(), request.getDescription())) changed.add("description");
        if (!Objects.equals(issue.getType(), request.getType())) changed.add("type");
        if (issue.getIssuePriority() != request.getIssuePriority()) changed.add("priority");
        if (!Objects.equals(issue.getDueDate(), request.getDueDate())) changed.add("dueDate");
        return changed;
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
        publish(AuditAction.CREATED, project.getId(), issue.getId(), issue.getTitle());
        return issueMapper.toResponse(issue);
    }

    public IssueResponse getIssueById(Long id) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
        checkCallerHasRole(issue.getProject().getId());
        return issueMapper.toResponse(issue);
    }

    public Page<IssueResponse> getIssuesByProject(Long projectId,IssueFilter filter, Pageable pageable) {
        checkCallerHasRole(projectId);
        PageableValidator.requireSortableBy(pageable, SORTABLE_FIELDS);
        return issueRepository.findAll(IssueSpecifications.matching(projectId, filter), pageable)
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
        List<String> changedFields = changedFields(existingIssue, request);

        Issue updatedIssue = issueMapper.updateEntity(request, existingIssue);
        issueRepository.save(updatedIssue);

        if (!changedFields.isEmpty()) {
            publish(AuditAction.UPDATED, projectId, id, "changed: " + String.join(", ", changedFields));
        }
        if (currentStatus != request.getIssueStatus()) {
            publish(AuditAction.STATUS_CHANGED, projectId, id, currentStatus + " -> " + request.getIssueStatus());
        }
        if (!Objects.equals(currentAssigneeId, request.getAssigneeId())) {
            publish(AuditAction.ASSIGNEE_CHANGED, projectId, id,
                    "assignee " + currentAssigneeId + " -> " + request.getAssigneeId());
        }
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
        publish(AuditAction.DELETED, issue.getProject().getId(), issue.getId(), issue.getTitle());
    }
}