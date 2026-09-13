package com.example.enterprise_issues_tracking_system.mapper;

import com.example.enterprise_issues_tracking_system.dto.IssueRequest;
import com.example.enterprise_issues_tracking_system.dto.IssueResponse;
import com.example.enterprise_issues_tracking_system.dto.IssueUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.Issue;
import com.example.enterprise_issues_tracking_system.entity.Project;
import com.example.enterprise_issues_tracking_system.entity.User;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class IssueMapper {

    public Issue toEntity(IssueRequest request) {

        Project project = new Project();
        project.setId(request.getProjectId());

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = new User();
            assignee.setId(request.getAssigneeId());
        }

        User reporter = new User();
        reporter.setId(request.getReporterId());

        Issue issue = new Issue();
        issue.setDescription(request.getDescription());
        issue.setProject(project);
        issue.setTitle(request.getTitle());
        issue.setType(request.getType());
        issue.setIssuePriority(request.getIssuePriority());
        issue.setAssignee(assignee);
        issue.setReporter(reporter);
        issue.setDueDate(request.getDueDate());

        return issue;
    }

    public Issue updateEntity(IssueUpdateRequest request, Issue existingIssue) {
        if (!Objects.equals(request.getProjectId(), existingIssue.getProject().getId())){
            Project project = new Project();
            project.setId(request.getProjectId());
            existingIssue.setProject(project);
        }
        if (!Objects.equals(request.getReporterId(), existingIssue.getReporter().getId())) {
            User reporter = new User();
            reporter.setId(request.getReporterId());
            existingIssue.setReporter(reporter);
        }
        Long currentAssigneeId = existingIssue.getAssignee() != null ? existingIssue.getAssignee().getId() : null;
        if (!Objects.equals(request.getAssigneeId(), currentAssigneeId)) {
            if (request.getAssigneeId() == null) {
                existingIssue.setAssignee(null);
            } else {
                User assignee = new User();
                assignee.setId(request.getAssigneeId());
                existingIssue.setAssignee(assignee);
            }
        }

        existingIssue.setDescription(request.getDescription());
        existingIssue.setTitle(request.getTitle());
        existingIssue.setType(request.getType());
        existingIssue.setIssuePriority(request.getIssuePriority());
        existingIssue.setDueDate(request.getDueDate());
        existingIssue.setIssueStatus(request.getIssueStatus());

        return existingIssue;
    }

    public IssueResponse toResponse(Issue issue) {
        IssueResponse response = new IssueResponse();
        response.setIssueStatus(issue.getIssueStatus());
        response.setId(issue.getId());
        response.setCreatedAt(issue.getCreatedAt());
        response.setProjectId(issue.getProject().getId());
        response.setDescription(issue.getDescription());
        response.setIssuePriority(issue.getIssuePriority());
        response.setType(issue.getType());
        response.setUpdatedAt(issue.getUpdatedAt());
        response.setTitle(issue.getTitle());
        response.setDueDate(issue.getDueDate());
        response.setAssigneeId(issue.getAssignee() != null ? issue.getAssignee().getId() : null);
        response.setCreatedAt(issue.getCreatedAt());

        return response;
    }
}
