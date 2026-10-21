package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.AttachmentResponse;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.InvalidFileException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.AttachmentMapper;
import com.example.enterprise_issues_tracking_system.repository.AttachmentRepository;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private AttachmentMapper attachmentMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AttachmentService attachmentService;

    private ProjectMember buildMembership(Long projectId, Long userId, Role role) {
        ProjectMemberId id = new ProjectMemberId();
        Project project = new Project();
        project.setId(projectId);
        User user = new User();
        user.setId(userId);
        id.setProject(project);
        id.setUser(user);
        ProjectMember member = new ProjectMember();
        member.setId(id);
        member.setRole(role);
        return member;
    }

    @Test
    public void createAttachment_whenAuthorized_shouldCreate() throws IOException {

        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);
        byte[] content = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
        String originalFileName = "screenshot.jpg";
        AttachmentResponse response = new AttachmentResponse();
        response.setIssueId(issueId);
        response.setOriginalFilename(originalFileName);
        response.setFileSize((long) content.length);

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        when(attachmentMapper.toResponse(any(Attachment.class))).thenReturn(response);
        AttachmentResponse result = attachmentService.createAttachment(content, originalFileName, issueId);

        ArgumentCaptor<Attachment> attachmentArgumentCaptor = ArgumentCaptor.forClass(Attachment.class);
        ArgumentCaptor<String> storageKeyCaptor = ArgumentCaptor.forClass(String.class);

        verify(attachmentRepository).save(attachmentArgumentCaptor.capture());
        verify(fileStorageService).store(storageKeyCaptor.capture(), eq(content));

        Attachment saved = attachmentArgumentCaptor.getValue();
        assertEquals("image/jpeg", saved.getContentType());
        assertEquals(originalFileName, saved.getOriginalFilename());
        assertEquals(issue, saved.getIssue());
        assertEquals(caller, saved.getUploadedBy());
        assertTrue(storageKeyCaptor.getValue().startsWith("issues/" + issueId + "/attachments/"));
        assertTrue(storageKeyCaptor.getValue().endsWith("-" + originalFileName));
        assertEquals(response, result);
    }

    @Test
    public void createAttachment_whenEmptyFile_shouldThrowException() throws IOException {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);
        byte[] content = new byte[0];
        String safeName = "screenshot.jpg";

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));

        assertThrows(InvalidFileException.class, () -> attachmentService.createAttachment(content, safeName, issueId));
        verify(fileStorageService, never()).store(any(), any());
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    public void createAttachment_whenOversizedFile_shouldThrowException() throws IOException {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);
        byte[] content = new byte[10 * 1024 * 1024 + 1];
        String safeName = "screenshot.jpg";

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));

        assertThrows(InvalidFileException.class, () -> attachmentService.createAttachment(content, safeName, issueId));
        verify(fileStorageService, never()).store(any(), any());
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    public void createAttachment_whenDetectingDisallowedFile_shouldThrowException() throws IOException {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);
        byte[] content = {0x4D, 0x5A, (byte) 0x90, 0x00, 0, 0, 0, 0};
        String safeName = "screenshot.jpg";

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));

        assertThrows(InvalidFileException.class, () -> attachmentService.createAttachment(content, safeName, issueId));
        verify(fileStorageService, never()).store(any(), any());
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    public void createAttachment_whenUnauthorized_shouldThrowException() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);
        byte[] content = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
        String originalFileName = "screenshot.jpg";

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> attachmentService.createAttachment(content, originalFileName, issueId));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    public void createAttachment_whenNotFound_shouldThrowException() {
        Long issueId = 1L;
        byte[] content = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
        String originalFileName = "screenshot.jpg";

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attachmentService.createAttachment(content, originalFileName, issueId));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    public void getAttachmentContent_whenAuthorized_shouldRespond() throws IOException {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long attachmentId = 3L;
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setIssue(issue);
        attachment.setStorageKey("issues/1/attachments/abc-screenshot.png");
        attachment.setOriginalFilename("screenshot.png");
        attachment.setContentType("image/png");
        InputStream stream = new ByteArrayInputStream("data".getBytes());
        Long callerId = 4L;
        User caller = new User();
        caller.setId(callerId);
        AttachmentContent attachmentContent = new AttachmentContent(stream, attachment.getOriginalFilename(), attachment.getContentType());

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.of(attachment));
        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        when(fileStorageService.retrieve(attachment.getStorageKey())).thenReturn(stream);
        AttachmentContent result = attachmentService.getAttachmentContent(attachmentId);

        assertEquals(attachmentContent, result);
        assertSame(stream, result.stream());
        assertEquals("screenshot.png", result.filename());
        assertEquals("image/png", result.contentType());
    }

    @Test
    public void getAttachmentContent_whenUnAuthorized_shouldThrowException() throws IOException {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 4L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long attachmentId = 1L;
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setIssue(issue);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.of(attachment));
        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> attachmentService.getAttachmentContent(attachmentId));

        verify(fileStorageService, never()).retrieve(any());
    }

    @Test
    public void getAttachmentContent_whenNotFound_shouldThrowException() {
        Long attachmentId = 1L;

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attachmentService.getAttachmentContent(attachmentId));
    }

    @Test
    public void getAttachmentMetadata_whenAuthorized_shouldRespond() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long attachmentId = 3L;
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setIssue(issue);
        Long callerId = 4L;
        User caller = new User();
        caller.setId(callerId);
        AttachmentResponse response = new AttachmentResponse();
        response.setIssueId(issueId);
        response.setId(attachmentId);

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.of(attachment));
        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        when(attachmentMapper.toResponse(attachment)).thenReturn(response);
        AttachmentResponse result = attachmentService.getAttachmentMetadata(attachmentId);

        assertEquals(response, result);
    }

    @Test
    public void getAttachmentMetadata_whenUnAuthorized_shouldThrowException() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long attachmentId = 3L;
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setIssue(issue);
        Long callerId = 4L;
        User caller = new User();
        caller.setId(callerId);

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.of(attachment));
        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> attachmentService.getAttachmentMetadata(attachmentId));
    }

    @Test
    public void getAttachmentMetadata_whenNotFound_shouldThrowException() {
        Long attachmentId = 1L;

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attachmentService.getAttachmentMetadata(attachmentId));

    }

    @Test
    public void getAttachmentByIssue_whenAuthorized_shouldRespond() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);
        Long attachment1Id = 4L;
        Attachment attachment1 = new Attachment();
        attachment1.setId(attachment1Id);
        attachment1.setIssue(issue);
        Long attachment2Id = 5L;
        Attachment attachment2 = new Attachment();
        attachment2.setId(attachment2Id);
        attachment2.setIssue(issue);
        List<Attachment> allAttachments = List.of(attachment1, attachment2);
        AttachmentResponse response1 = new AttachmentResponse();
        response1.setId(attachment1Id);
        response1.setIssueId(issueId);
        AttachmentResponse response2 = new AttachmentResponse();
        response2.setId(attachment2Id);
        response2.setIssueId(issueId);

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        when(attachmentRepository.findAllByIssueIdAndDeletedAtIsNull(issueId)).thenReturn(allAttachments);
        when(attachmentMapper.toResponse(attachment1)).thenReturn(response1);
        when(attachmentMapper.toResponse(attachment2)).thenReturn(response2);
        List<AttachmentResponse> result = attachmentService.getAttachmentsByIssue(issueId);

        assertEquals(List.of(response1, response2), result);
    }

    @Test
    public void getAttachmentByIssue_whenUnauthorized_shouldThrowException() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> attachmentService.getAttachmentsByIssue(issueId));
    }

    @Test
    public void deleteAttachment_whenCallerIsAdmin_shouldDelete() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 4L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long uploaderId = 5L;
        User uploader = new User();
        uploader.setId(uploaderId);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);
        Long attachmentId = 1L;
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setUploadedBy(uploader);
        attachment.setIssue(issue);

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.of(attachment));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.ADMIN)));
        attachmentService.deleteAttachment(attachmentId);

        verify(attachmentRepository).save(attachment);
        assertNotNull(attachment.getDeletedAt());
    }

    @Test
    public void deleteAttachment_whenCallerIsManager_shouldDelete() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 4L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long uploaderId = 5L;
        User uploader = new User();
        uploader.setId(uploaderId);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);
        Long attachmentId = 1L;
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setUploadedBy(uploader);
        attachment.setIssue(issue);

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.of(attachment));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.PROJECT_MANAGER)));
        attachmentService.deleteAttachment(attachmentId);

        verify(attachmentRepository).save(attachment);
        assertNotNull(attachment.getDeletedAt());
    }

    @Test
    public void deleteAttachment_whenCallerIsUploader_shouldDelete() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 4L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);
        Long attachmentId = 1L;
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setUploadedBy(caller);
        attachment.setIssue(issue);

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.of(attachment));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        attachmentService.deleteAttachment(attachmentId);

        verify(attachmentRepository).save(attachment);
        assertNotNull(attachment.getDeletedAt());
    }

    @Test
    public void deleteAttachment_whenUnauthorized_shouldThrowException() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 4L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);
        Long uploaderId = 5L;
        User uploader = new User();
        uploader.setId(uploaderId);
        Long attachmentId = 1L;
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setUploadedBy(uploader);
        attachment.setIssue(issue);

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.of(attachment));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> attachmentService.deleteAttachment(attachmentId));
        assertNull(attachment.getDeletedAt());
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    public void deleteAttachment_whenNotFound_shouldThrowException() {
        Long attachmentId = 1L;

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attachmentService.deleteAttachment(attachmentId));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    public void deleteAttachment_whenProjectNotFound_shouldThrowException() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Issue issue = new Issue();
        issue.setId(4L);
        issue.setProject(project);
        Long attachmentId = 1L;
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setIssue(issue);

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.of(attachment));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attachmentService.deleteAttachment(attachmentId));
        verify(attachmentRepository, never()).save(any());
        assertNull(attachment.getDeletedAt());
    }

    @Test
    public void deleteAttachment_whenDeveloperIsNotUploader_shouldThrowException() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 4L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long uploaderId = 5L;
        User uploader = new User();
        uploader.setId(uploaderId);
        Long callerId = 3L;
        User caller = new User();
        caller.setId(callerId);
        Long attachmentId = 1L;
        Attachment attachment = new Attachment();
        attachment.setId(attachmentId);
        attachment.setUploadedBy(uploader);
        attachment.setIssue(issue);

        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId)).thenReturn(Optional.of(attachment));
        when(projectRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(project));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));

        assertThrows(AccessDeniedException.class, () -> attachmentService.deleteAttachment(attachmentId));
        assertNull(attachment.getDeletedAt());
        verify(attachmentRepository, never()).save(any());
    }
}