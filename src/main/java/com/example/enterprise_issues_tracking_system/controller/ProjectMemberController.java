package com.example.enterprise_issues_tracking_system.controller;

import com.example.enterprise_issues_tracking_system.dto.ProjectMemberRequest;
import com.example.enterprise_issues_tracking_system.dto.ProjectMemberResponse;
import com.example.enterprise_issues_tracking_system.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects/{id}/users")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @PostMapping
    public ResponseEntity<ProjectMemberResponse> addMember(@PathVariable Long id, @Valid @RequestBody ProjectMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectMemberService.addMember(id, request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProjectMemberResponse> getMember(@PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(projectMemberService.getMember(id, userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ProjectMemberResponse> updateMember(@PathVariable Long id, @PathVariable Long userId, @Valid @RequestBody ProjectMemberRequest request) {
        return ResponseEntity.ok(projectMemberService.updateMember(id, userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id, @PathVariable Long userId) {
        projectMemberService.deleteMember(id, userId);
        return ResponseEntity.noContent().build();
    }
}
