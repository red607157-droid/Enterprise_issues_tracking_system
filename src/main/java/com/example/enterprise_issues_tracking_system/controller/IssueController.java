package com.example.enterprise_issues_tracking_system.controller;

import com.example.enterprise_issues_tracking_system.dto.IssueRequest;
import com.example.enterprise_issues_tracking_system.dto.IssueResponse;
import com.example.enterprise_issues_tracking_system.dto.IssueUpdateRequest;
import com.example.enterprise_issues_tracking_system.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @PostMapping("/issues")
    public ResponseEntity<IssueResponse> createIssue(@Valid @RequestBody IssueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.createIssue(request));
    }

    @GetMapping("/issues/{id}")
    public ResponseEntity<IssueResponse> getIssueById(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.getIssueById(id));
    }

    @GetMapping("/projects/{projectId}/issues")
    public ResponseEntity<Page<IssueResponse>> getIssuesByProject(@PageableDefault Pageable pageable, @PathVariable Long projectId) {
        return ResponseEntity.ok(issueService.getIssuesByProject(projectId, pageable));
    }

    @PutMapping("/issues/{id}")
    public ResponseEntity<IssueResponse> updateIssue(@PathVariable Long id, @Valid @RequestBody IssueUpdateRequest request) {
        return ResponseEntity.ok(issueService.updateIssue(id, request));
    }

    @DeleteMapping("/issues/{id}")
    public ResponseEntity<Void> deleteIssue(@PathVariable Long id) {
        issueService.deleteIssue(id);
        return ResponseEntity.noContent().build();
    }
}
