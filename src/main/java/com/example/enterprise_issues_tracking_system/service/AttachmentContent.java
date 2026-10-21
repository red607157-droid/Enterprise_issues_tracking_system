package com.example.enterprise_issues_tracking_system.service;

import java.io.InputStream;

public record AttachmentContent(InputStream stream, String filename, String contentType) {
}
