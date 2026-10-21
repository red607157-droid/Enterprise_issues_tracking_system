package com.example.enterprise_issues_tracking_system.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path basePath;

    private Path resolvePath(String storageKey) throws IOException {
        Path target = basePath.resolve(storageKey).normalize();
        if (!target.startsWith(basePath)) {
            throw new IOException("Invalid storage key: " + storageKey);
        }
        return target;
    }

    public LocalFileStorageService(@Value("${app.storage.local-path}") String uploadDir) {
        this.basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(basePath);
    }

    @Override
    public void store(String storageKey, byte[] content) throws IOException{
        Path target = resolvePath(storageKey);
        Files.createDirectories(target.getParent());
        Files.write(target, content, StandardOpenOption.CREATE_NEW);

    }

    @Override
    public InputStream retrieve(String storageKey) throws IOException {
        Path target = resolvePath(storageKey);
        return Files.newInputStream(target);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Path target = resolvePath(storageKey);
        Files.deleteIfExists(target);
    }
}
