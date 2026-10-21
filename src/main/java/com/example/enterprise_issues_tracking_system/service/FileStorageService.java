package com.example.enterprise_issues_tracking_system.service;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {

    void store(String storageKey, byte[] content) throws IOException;

    InputStream retrieve(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}