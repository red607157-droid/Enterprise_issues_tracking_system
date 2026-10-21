package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.AttachmentResponse;
import com.example.enterprise_issues_tracking_system.entity.Attachment;
import com.example.enterprise_issues_tracking_system.entity.Issue;
import com.example.enterprise_issues_tracking_system.entity.Project;
import com.example.enterprise_issues_tracking_system.entity.Role;
import com.example.enterprise_issues_tracking_system.exception.InvalidFileException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.AttachmentMapper;
import com.example.enterprise_issues_tracking_system.repository.AttachmentRepository;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AttachmentRepository attachmentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AttachmentMapper attachmentMapper;
    private final FileStorageService fileStorageService;
    private final Tika tika = new Tika();
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES   = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf", "text/plain");

    private Issue IsMember(Long issueId) {
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue ID not found"));
        Project project = projectRepository.findByIdAndDeletedAtIsNull(issue.getProject().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        Long callerId = currentUserProvider.getCurrentUser().getId();
        projectMemberRepository.findMembership(project.getId(), callerId)
                .orElseThrow(() -> new AccessDeniedException("You don't allowed to do this"));
        return issue;
    }

    public AttachmentResponse createAttachment(byte[] content, String originFileName, Long issueId) throws IOException {
        Issue issue = IsMember(issueId);
        if (content.length == 0) {
            throw new InvalidFileException("File is empty");
        }
        if (content.length > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size exceeded limit of file size");
        }
        String detectedFile = tika.detect(content);
        if (!ALLOWED_TYPES.contains(detectedFile)) {
            throw new InvalidFileException("File type is not allowed");
        }
        String safeName = Paths.get(originFileName).getFileName().toString();
        String storageKey = "issues/" + issueId + "/attachments/" + UUID.randomUUID() + "-" + safeName;
        fileStorageService.store(storageKey, content);
        Attachment attachment = Attachment.builder()
                .fileSize((long) content.length)
                .originalFilename(originFileName)
                .contentType(detectedFile)
                .issue(issue)
                .storageKey(storageKey)
                .uploadedBy(currentUserProvider.getCurrentUser())
                .build();
        attachmentRepository.save(attachment);
        return attachmentMapper.toResponse(attachment);
    }

    public AttachmentResponse getAttachmentMetadata(Long id) {
        Attachment attachment = attachmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment ID not found"));
        IsMember(attachment.getIssue().getId());
        return attachmentMapper.toResponse(attachment);
    }

    public AttachmentContent getAttachmentContent(Long id) throws IOException {
        Attachment attachment = attachmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment ID not found"));
        IsMember(attachment.getIssue().getId());
        InputStream stream = fileStorageService.retrieve(attachment.getStorageKey());
        return new AttachmentContent(stream, attachment.getOriginalFilename(), attachment.getContentType());
    }

    public List<AttachmentResponse> getAttachmentsByIssue(Long issueId) {
        IsMember(issueId);
        return attachmentRepository.findAllByIssueIdAndDeletedAtIsNull(issueId).stream()
                .map(attachmentMapper::toResponse)
                .toList();
    }

    public void deleteAttachment(Long id) {
        Attachment attachment = attachmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment ID not found"));
        Long projectId = attachment.getIssue().getProject().getId();
        projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project ID not found"));
        Long callerId = currentUserProvider.getCurrentUser().getId();
        boolean isUploader = attachment.getUploadedBy().getId().equals(callerId);
        boolean isAdminOrManager = projectMemberRepository.findMembership(projectId, callerId)
                .map(projectMember -> List.of(Role.ADMIN, Role.PROJECT_MANAGER).contains(projectMember.getRole()))
                .orElse(false);
        if (!(isAdminOrManager || isUploader)) {
            throw new AccessDeniedException("You are not allowed to do this");
        }
        attachment.softDelete();
        attachmentRepository.save(attachment);
    }

}
