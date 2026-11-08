package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.ActivityLogResponse;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.ActivityLogMapper;
import com.example.enterprise_issues_tracking_system.repository.*;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import com.example.enterprise_issues_tracking_system.util.PageableValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final static Set<String> SORTED_FIELDS = Set.of("id", "createdAt", "action", "entityType");
    private final CurrentUserProvider currentUserProvider;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogMapper activityLogMapper;

    public Page<ActivityLogResponse> getIssueActivity(Long issueId, Pageable pageable) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
        requireMember(issue.getProject().getId());
        PageableValidator.requireSortableBy(pageable, SORTED_FIELDS);
        return activityLogRepository.findAllByIssueId(issueId, pageable)
                .map(activityLogMapper::toResponse);
    }

    public Page<ActivityLogResponse> getProjectActivity(Long projectId, Pageable pageable, AuditEntityType entityType, Long actorId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        ProjectMember membership = requireMember(projectId);
        if (!List.of(Role.ADMIN, Role.PROJECT_MANAGER).contains(membership.getRole())) {
            throw new AccessDeniedException("You are not allowed to do this");
        }
        PageableValidator.requireSortableBy(pageable, SORTED_FIELDS);
        return activityLogRepository.findAll(ActivityLogSpecification.forProject(projectId, entityType, actorId), pageable)
                .map(activityLogMapper::toResponse);
    }

    private ProjectMember requireMember(Long projectId) {
        Long callerId = currentUserProvider.getCurrentUser().getId();
        return projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new  AccessDeniedException("You are not allowed to do this"));
    }
}

