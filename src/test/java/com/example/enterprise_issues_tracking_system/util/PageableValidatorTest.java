package com.example.enterprise_issues_tracking_system.util;

import com.example.enterprise_issues_tracking_system.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PageableValidatorTest {

    private static final Set<String> ALLOWED = Set.of("title", "createdAt");

    @Test
    void requireSortableBy_whenUnsorted_shouldPass() {
        assertDoesNotThrow(() -> PageableValidator.requireSortableBy(PageRequest.of(0, 10), ALLOWED));
    }

    @Test
    void requireSortableBy_whenAllFieldsAllowed_shouldPass() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("title").ascending().and(Sort.by("createdAt").descending()));

        assertDoesNotThrow(() -> PageableValidator.requireSortableBy(pageable, ALLOWED));
    }

    @Test
    void requireSortableBy_whenFieldNotAllowed_shouldThrowAndNameTheField() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("reporter.password"));

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> PageableValidator.requireSortableBy(pageable, ALLOWED));

        assertTrue(exception.getMessage().contains("reporter.password"));
        assertTrue(exception.getMessage().contains("createdAt, title"));
    }

    @Test
    void requireSortableBy_whenOneOfSeveralFieldsNotAllowed_shouldThrow() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("title", "password"));

        assertThrows(InvalidRequestException.class, () -> PageableValidator.requireSortableBy(pageable, ALLOWED));
    }
}