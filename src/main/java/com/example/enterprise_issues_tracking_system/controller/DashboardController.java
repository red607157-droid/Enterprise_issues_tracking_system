package com.example.enterprise_issues_tracking_system.controller;

import com.example.enterprise_issues_tracking_system.dto.AssigneeWorkloadResponse;
import com.example.enterprise_issues_tracking_system.dto.BurndownResponse;
import com.example.enterprise_issues_tracking_system.dto.IssueByStatusResponse;
import com.example.enterprise_issues_tracking_system.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/issue-by-status")
    public ResponseEntity<IssueByStatusResponse> getIssuesByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(dashboardService.getIssuesByProject(projectId));
    }

    @GetMapping("/issues-by-assignee")
    public ResponseEntity<List<AssigneeWorkloadResponse>> getWorkloadByAssignee(@PathVariable Long projectId) {
        return ResponseEntity.ok(dashboardService.getWorkloadByAssignee(projectId));
    }

    @GetMapping("/burndown")
    public ResponseEntity<BurndownResponse> getBurndown(@PathVariable Long projectId,
                                                        @RequestParam(required = false) @DateTimeFormat (iso = DateTimeFormat.ISO.DATE)LocalDate from,
                                                        @RequestParam(required = false) @DateTimeFormat (iso = DateTimeFormat.ISO.DATE)LocalDate to) {
        return ResponseEntity.ok(dashboardService.getBurnDown(projectId, from, to));
    }
}
