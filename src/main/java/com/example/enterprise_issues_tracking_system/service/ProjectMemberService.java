package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.ProjectMemberRequest;
import com.example.enterprise_issues_tracking_system.dto.ProjectMemberResponse;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.DuplicateResourceException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.ProjectMemberMapper;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.repository.UserRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberMapper projectMemberMapper;
    private final CurrentUserProvider currentUserProvider;

    public ProjectMemberResponse addMember(Long id, ProjectMemberRequest request) {
        Project project = findActiveProject(id);
        Role callerRole = requireAdminOrManager(id);
        checkCanAssignRole(callerRole, request.getRole());
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User ID not found"));
        if (projectMemberRepository.findMembership(id, user.getId()).isPresent()) {
            throw new DuplicateResourceException("User is already a member of this project");
        }
        ProjectMember projectMember = projectMemberMapper.toEntity(request, user, project);
        projectMemberRepository.save(projectMember);
        return projectMemberMapper.toResponse(projectMember);
    }

    public ProjectMemberResponse getMember(Long id, Long userId) {
        findActiveProject(id);
        requireAdminOrManager(id);
        ProjectMember projectMember = findMember(id, userId);
        return projectMemberMapper.toResponse(projectMember);
    }

    public ProjectMemberResponse updateMember(Long id, Long userId, ProjectMemberRequest request) {
        Project project = findActiveProject(id);
        Role callerRole = requireAdminOrManager(id);
        ProjectMember projectMember = findMember(id, userId);
        checkCanModifyTarget(callerRole, project, userId, projectMember);
        checkCanAssignRole(callerRole, request.getRole());
        projectMember.setRole(request.getRole());
        projectMemberRepository.save(projectMember);
        return projectMemberMapper.toResponse(projectMember);
    }

    public void deleteMember(Long id, Long userId) {
        Project project = findActiveProject(id);
        Role callerRole = requireAdminOrManager(id);
        ProjectMember projectMember = findMember(id, userId);
        checkCanModifyTarget(callerRole, project, userId, projectMember);
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

    private Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
    }

    private ProjectMember findMember(Long projectId, Long userId) {
        return projectMemberRepository.findMembership(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID or user ID not found"));
    }

        private Role requireAdminOrManager(Long projectId) {
        Long callerId = currentUserProvider.getCurrentUser().getId();
        ProjectMember callerMembership = projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new AccessDeniedException("You're not allowed to do this"));
        if (!List.of(Role.ADMIN, Role.PROJECT_MANAGER).contains(callerMembership.getRole())) {
            throw new AccessDeniedException("You are not allowed to do this");
        }
        return callerMembership.getRole();
    }

    private void checkCanAssignRole(Role callerRole, Role roleToAssign) {
        if (roleToAssign == Role.ADMIN && callerRole != Role.ADMIN) {
            throw new AccessDeniedException("Only an admin can grant the ADMIN role");
        }
    }

    private void checkCanModifyTarget(Role callerRole, Project project, Long targetUserId, ProjectMember target) {
        if (project.getOwner() != null && Objects.equals(project.getOwner().getId(), targetUserId)) {
            throw new AccessDeniedException("The project owner cannot be modified or removed");
        }
        if (target.getRole() == Role.ADMIN && callerRole != Role.ADMIN) {
            throw new AccessDeniedException("Only an admin can modify or remove an admin");
        }
    }
}