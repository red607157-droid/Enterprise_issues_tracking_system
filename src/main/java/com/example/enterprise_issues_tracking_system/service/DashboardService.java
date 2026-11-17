package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.*;
import com.example.enterprise_issues_tracking_system.entity.AuditAction;
import com.example.enterprise_issues_tracking_system.entity.AuditEntityType;
import com.example.enterprise_issues_tracking_system.entity.IssueStatus;
import com.example.enterprise_issues_tracking_system.exception.InvalidRequestException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.repository.ActivityLogRepository;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    static final int DEFAULT_BURNDOWN_DAYS = 14;
    static final int MAX_BURNDOWN_DAYS = 92;
    static final String UNASSIGNED_NAME = "Unassigned";

    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectMemberRepository projectMemberRepository;
    private final IssueRepository issueRepository;
    private final ActivityLogRepository activityLogRepository;

    private void requireMember(Long projectId) {
        projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        Long callerId = currentUserProvider.getCurrentUser().getId();
        projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to do this"));
    }

    public IssueByStatusResponse getIssuesByProject(Long projectId) {
        requireMember(projectId);
        Map<IssueStatus, Long> counts = new EnumMap<>(IssueStatus.class);
        for (IssueStatus status : IssueStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : issueRepository.countByStatus(projectId)) {
            counts.put((IssueStatus) row[0], (Long) row[1]);
        }
        List<StatusCountResponse> statuses = counts.entrySet().stream()
                .map(entry -> new StatusCountResponse(entry.getKey(), entry.getValue()))
                .toList();
        long total =  statuses.stream().mapToLong(StatusCountResponse::count).sum();
        return new IssueByStatusResponse(total, statuses);
    }

    public List<AssigneeWorkloadResponse> getWorkloadByAssignee(Long projectId) {
        requireMember(projectId);
        return issueRepository.countByAssignee(projectId, IssueStatus.DONE).stream()
                .map(row -> {
                    Long assigneeId = row[0] == null ? null : (long) row[0];
                    String name = assigneeId == null ? UNASSIGNED_NAME : (String) row[1];
                    long total = ((Number) row[2]).longValue();
                    long done = row[3] == null ? 0 : ((Number) row[3]).longValue();
                    return new AssigneeWorkloadResponse(assigneeId, name, total,total - done);
                })
                .sorted(Comparator.comparing(AssigneeWorkloadResponse::open).reversed().thenComparing(AssigneeWorkloadResponse::assigneeName))
                .toList();
    }

    public BurndownResponse getBurnDown(Long projectId, LocalDate fromParam, LocalDate toParam) {
        requireMember(projectId);
        LocalDate to = toParam != null ? toParam : LocalDate.now();
        LocalDate from = fromParam != null ? fromParam : to.minusDays(DEFAULT_BURNDOWN_DAYS - 1);
        if (from.isAfter(to)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_BURNDOWN_DAYS) {
            throw new InvalidRequestException("The date range cannot be longer than " + MAX_BURNDOWN_DAYS + " days");
        }
        List<BurndownEvent> events = activityLogRepository.findBurnDownEvents(projectId, AuditEntityType.ISSUE,
                List.of(AuditAction.CREATED, AuditAction.STATUS_CHANGED, AuditAction.DELETED),
                to.plusDays(1).atStartOfDay());
        return new BurndownResponse(from, to, BurndownCalculator.calculate(events, from, to));
    }
}
