package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.LabelRequest;
import com.example.enterprise_issues_tracking_system.dto.LabelResponse;
import com.example.enterprise_issues_tracking_system.dto.LabelUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.Label;
import com.example.enterprise_issues_tracking_system.entity.Project;
import com.example.enterprise_issues_tracking_system.entity.ProjectMember;
import com.example.enterprise_issues_tracking_system.entity.Role;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.LabelMapper;
import com.example.enterprise_issues_tracking_system.repository.LabelRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelMapper labelMapper;
    private final LabelRepository labelRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    private void IsAMember(Long projectId) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long callerId = userPrincipal.getUser().getId();
        ProjectMember projectMember = projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to do this"));
    }

    private void IsAdminOrManager(Long projectId) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long callerId = userPrincipal.getUser().getId();
        ProjectMember projectMember = projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to do this"));
        if (!List.of(Role.ADMIN, Role.PROJECT_MANAGER).contains(projectMember.getRole())) {
            throw new AccessDeniedException("You are not allowed to do this");
        }
    }

    public LabelResponse createLabel(LabelRequest request) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(request.getProjectId())
                        .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        IsAdminOrManager(request.getProjectId());
        Label label = labelMapper.toEntity(request);
        labelRepository.save(label);
        return labelMapper.toResponse(label);
    }

    public LabelResponse getLabelById(Long id) {
        Label label = labelRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Label ID not found"));
        IsAMember(label.getProject().getId());
        return labelMapper.toResponse(label);
    }

    public List<LabelResponse> getLabelsByProject(Long projectId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        IsAMember(projectId);
        return labelRepository.findAllByProjectIdAndDeletedAtIsNull(projectId)
                .stream().map(labelMapper::toResponse).toList();
    }

    public LabelResponse updateLabel(Long id, LabelUpdateRequest request) {
        Label existingLabel = labelRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Label ID not found"));
        IsAdminOrManager(existingLabel.getProject().getId());
        Label newLabel = labelMapper.updateEntity(request, existingLabel);
        labelRepository.save(newLabel);
        return labelMapper.toResponse(newLabel);
    }

    public void deleteLabel(Long id) {
        Label label = labelRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Label ID not found"));
        IsAdminOrManager(label.getProject().getId());
        label.softDelete();
        labelRepository.save(label);
    }

}
