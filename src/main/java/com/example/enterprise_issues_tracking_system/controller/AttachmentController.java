package com.example.enterprise_issues_tracking_system.controller;

import com.example.enterprise_issues_tracking_system.dto.AttachmentResponse;
import com.example.enterprise_issues_tracking_system.service.AttachmentContent;
import com.example.enterprise_issues_tracking_system.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/issues/{issueId}/attachments")
    public ResponseEntity<AttachmentResponse> createAttachment(@PathVariable Long issueId, @RequestParam("file")MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.createAttachment(file.getBytes(), file.getOriginalFilename(), issueId));
    }

    @GetMapping("/issues/{issueId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachmentsByIssue(@PathVariable Long issueId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByIssue(issueId));
    }

    @GetMapping("/attachments/{id}/content")
    public ResponseEntity<InputStreamResource> getAttachmentContent(@PathVariable Long id) throws IOException {
        AttachmentContent content = attachmentService.getAttachmentContent(id);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(content.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new InputStreamResource(content.stream()));
    }

    @GetMapping("/attachments/{id}")
    public ResponseEntity<AttachmentResponse> getAttachmentMetadata(@PathVariable Long id) {
        return ResponseEntity.ok(attachmentService.getAttachmentMetadata(id));
    }

    @DeleteMapping("/attachments/{id}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id) {
        attachmentService.deleteAttachment(id);
        return ResponseEntity.noContent().build();
    }
}
