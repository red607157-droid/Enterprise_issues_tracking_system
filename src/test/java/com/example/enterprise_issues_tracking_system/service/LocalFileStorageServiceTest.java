package com.example.enterprise_issues_tracking_system.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    private LocalFileStorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void SetUp() throws IOException {
        storageService = new LocalFileStorageService(tempDir.toString());
    }

    @Test
    void storeAndRetrieve_roundTripsExactBytes() throws IOException {
        String storageKey = "issues/1/attachments/test-file.txt";
        byte[] original = "hello attachment".getBytes(StandardCharsets.UTF_8);

        storageService.store(storageKey, original);

        byte[] retrieved;
        try (InputStream in = storageService.retrieve(storageKey)) {
            retrieved = in.readAllBytes();
        }

        assertArrayEquals(original, retrieved);
    }

    @Test
    void storeTheSameKeyTwice_shouldThrowIOException() throws IOException {
        String storageKey = "issues/1/attachments/test-file.txt";
        byte[] original = "hello attachment".getBytes(StandardCharsets.UTF_8);

        storageService.store(storageKey, original);

        assertThrows(FileAlreadyExistsException.class, () -> storageService.store(storageKey, original));
    }

    @Test
    void deleteAndRetrieveTheSameKey_shouldThrowIOException() throws IOException {
        String storageKey = "issues/1/attachments/test-file.txt";
        byte[] original = "hello attachment".getBytes(StandardCharsets.UTF_8);

        storageService.store(storageKey, original);
        storageService.delete(storageKey);

        assertThrows(NoSuchFileException.class, () -> storageService.retrieve(storageKey));
    }

    @Test
    void deleteAnUnexistedKey_shouldReturnNormally() throws IOException {
        String storageKey = "issues/1/attachments/test-file.txt";

        storageService.delete(storageKey);
    }

    @Test
    void storeRetrieveDelete_withTraversalKey_allThrowAndNothingEscapesTempDir() throws IOException {
        String maliciousKey = "../../../../../../../../tmp/evil-" + System.nanoTime() + ".txt";
        byte[] content = "pwned".getBytes(StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> storageService.store(maliciousKey, content));
        assertThrows(IOException.class, () -> storageService.retrieve(maliciousKey));
        assertThrows(IOException.class, () -> storageService.delete(maliciousKey));

        Path escapedTarget = tempDir.resolve(maliciousKey).normalize();
        assertFalse(Files.exists(escapedTarget));
    }
}