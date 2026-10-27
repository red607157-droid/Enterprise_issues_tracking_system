package com.example.enterprise_issues_tracking_system.controller;

import com.example.enterprise_issues_tracking_system.dto.LabelResponse;
import com.example.enterprise_issues_tracking_system.service.IssueLabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/issues/{issueId}/labels")
@RequiredArgsConstructor
public class IssueLabelController {

    private final IssueLabelService issueLabelService;

    @PostMapping("/{labelId}")
    public ResponseEntity<Void> addLabel(@PathVariable Long issueId, @PathVariable Long labelId) {
        issueLabelService.addLabel(issueId, labelId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<LabelResponse>> getLabelsByIssue(@PathVariable Long issueId) {
        return ResponseEntity.ok(issueLabelService.getLabelsByIssue(issueId));
    }

    @DeleteMapping("/{labelId}")
    public ResponseEntity<Void> removeLabel(@PathVariable Long issueId, @PathVariable Long labelId) {
        issueLabelService.removeLabel(issueId, labelId);
        return ResponseEntity.noContent().build();
    }
}