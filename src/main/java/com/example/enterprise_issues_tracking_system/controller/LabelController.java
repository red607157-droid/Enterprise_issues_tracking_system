package com.example.enterprise_issues_tracking_system.controller;

import com.example.enterprise_issues_tracking_system.dto.LabelRequest;
import com.example.enterprise_issues_tracking_system.dto.LabelResponse;
import com.example.enterprise_issues_tracking_system.dto.LabelUpdateRequest;
import com.example.enterprise_issues_tracking_system.service.LabelService;
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
public class LabelController {

    private final LabelService labelService;

    @PostMapping("/labels")
    public ResponseEntity<LabelResponse> createLabel(@Valid @RequestBody LabelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labelService.createLabel(request));
    }

    @GetMapping("/labels/{id}")
    public ResponseEntity<LabelResponse> getLabelById(@PathVariable Long id) {
        return ResponseEntity.ok(labelService.getLabelById(id));
    }

    @GetMapping("/projects/{projectId}/labels")
    public ResponseEntity<Page<LabelResponse>> getLabelsByProject(
            @PathVariable Long projectId,
            @PageableDefault(size = 20, sort = "name")Pageable pageable
    ) {
        return ResponseEntity.ok(labelService.getLabelsByProject(projectId, pageable));
    }

    @PutMapping("/labels/{id}")
    public ResponseEntity<LabelResponse> updateLabel(@PathVariable Long id, @Valid @RequestBody LabelUpdateRequest request) {
        return ResponseEntity.ok(labelService.updateLabel(id, request));
    }

    @DeleteMapping("/labels/{id}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long id) {
        labelService.deleteLabel(id);
        return ResponseEntity.noContent().build();
    }
}
