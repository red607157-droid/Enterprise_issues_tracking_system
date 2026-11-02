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
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import com.example.enterprise_issues_tracking_system.util.PageableValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelMapper labelMapper;
    private final LabelRepository labelRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private static final Set<String> SORTED_FIELDS = Set.of("id", "name", "color", "createdAt");

    private void IsAMember(Long projectId) {
        Long callerId = currentUserProvider.getCurrentUser().getId();
        ProjectMember projectMember = projectMemberRepository.findMembership(projectId, callerId)
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to do this"));
    }

    private void IsAdminOrManager(Long projectId) {
        Long callerId = currentUserProvider.getCurrentUser().getId();
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

    public Page<LabelResponse> getLabelsByProject(Long projectId, Pageable pageable) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        IsAMember(projectId);
        PageableValidator.requireSortableBy(pageable, SORTED_FIELDS);
        return labelRepository.findAllByProjectIdAndDeletedAtIsNull(projectId, pageable)
            .map(labelMapper::toResponse);
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
