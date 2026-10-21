package com.example.enterprise_issues_tracking_system.mapper;

import com.example.enterprise_issues_tracking_system.dto.AttachmentResponse;
import com.example.enterprise_issues_tracking_system.entity.Attachment;
import org.springframework.stereotype.Component;

@Component
public class AttachmentMapper {

    public AttachmentResponse toResponse(Attachment attachment) {

        AttachmentResponse response = new AttachmentResponse();
        response.setId(attachment.getId());
        response.setIssueId(attachment.getIssue().getId());
        response.setUploadedBy(attachment.getUploadedBy().getId());
        response.setOriginalFilename(attachment.getOriginalFilename());
        response.setContentType(attachment.getContentType());
        response.setFileSize(attachment.getFileSize());
        response.setCreatedAt(attachment.getCreatedAt());

        return response;
    }

}