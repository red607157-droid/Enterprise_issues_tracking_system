package com.example.enterprise_issues_tracking_system.controller;

import com.example.enterprise_issues_tracking_system.dto.ActivityLogResponse;
import com.example.enterprise_issues_tracking_system.entity.AuditEntityType;
import com.example.enterprise_issues_tracking_system.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping("issues/{issueId}/activity")
    public ResponseEntity<Page<ActivityLogResponse>> getIssueActivity(
            @PathVariable Long issueId,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getIssueActivity(issueId, pageable));
    }

    @GetMapping("projects/{projectId}/activity")
    public ResponseEntity<Page<ActivityLogResponse>> getProjectActivity(
            @PathVariable Long projectId,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) Long actorId,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)Pageable pageable) {
        return ResponseEntity.ok(activityLogService.getProjectActivity(projectId,pageable,entityType, actorId));
    }

}
