package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.ProjectMemberRequest;
import com.example.enterprise_issues_tracking_system.dto.ProjectMemberResponse;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.ProjectMemberMapper;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.repository.UserRepository;
import com.example.enterprise_issues_tracking_system.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberMapper projectMemberMapper;

    public ProjectMemberResponse addMember(Long id, ProjectMemberRequest request) {
        checkCallerHasRole(id, Role.ADMIN, Role.PROJECT_MANAGER);
        Project project = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User ID not found"));
        ProjectMember projectMember = projectMemberMapper.toEntity(request, user, project);
        projectMemberRepository.save(projectMember);
        return projectMemberMapper.toResponse(projectMember);
    }

    public ProjectMemberResponse getMember(Long id, Long userId) {
        checkCallerHasRole(id, Role.ADMIN, Role.PROJECT_MANAGER);
        ProjectMember projectMember = projectMemberRepository.findMembership(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID or user ID not found"));
        return projectMemberMapper.toResponse(projectMember);
    }

    public ProjectMemberResponse updateMember(Long id, Long userId, ProjectMemberRequest request) {
        checkCallerHasRole(id, Role.ADMIN, Role.PROJECT_MANAGER);
        ProjectMember projectMember = projectMemberRepository.findMembership(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID or user ID not found"));
        projectMember.setRole(request.getRole());
        return projectMemberMapper.toResponse(projectMemberRepository.save(projectMember));
    }

    public void deleteMember(Long id, Long userId) {
        checkCallerHasRole(id, Role.ADMIN, Role.PROJECT_MANAGER);
        ProjectMember projectMember = projectMemberRepository.findMembership(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID or user ID not found"));
        projectMemberRepository.delete(projectMember);
    }

    public void addOwnerAsAdmin(Project project, User user) {
        ProjectMemberId projectMemberId = new ProjectMemberId();
        projectMemberId.setProject(project);
        projectMemberId.setUser(user);

        ProjectMember projectMember = new ProjectMember();
        projectMember.setId(projectMemberId);
        projectMember.setRole(Role.ADMIN);
        projectMember.setCreatedAt(LocalDateTime.now());

        projectMemberRepository.save(projectMember);
    }

    private void checkCallerHasRole(Long projectId, Role... allowedRoles) {
        projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long callerId = userPrincipal.getUser().getId();
        ProjectMember projectMember = projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new AccessDeniedException("You're not allowed to do this"));
        if (!List.of(allowedRoles).contains(projectMember.getRole())) {
            throw new AccessDeniedException("You are not allowed to do this");
        }
    }



}
