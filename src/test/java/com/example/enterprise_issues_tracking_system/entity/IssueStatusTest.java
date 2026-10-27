package com.example.enterprise_issues_tracking_system.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class IssueStatusTest {

    @ParameterizedTest
    @CsvSource({
            "OPEN, OPEN",
            "OPEN, IN_PROGRESS",
            "IN_PROGRESS, IN_PROGRESS",
            "IN_PROGRESS, OPEN",
            "IN_PROGRESS, IN_REVIEW",
            "IN_REVIEW, IN_REVIEW",
            "IN_REVIEW, IN_PROGRESS",
            "IN_REVIEW, DONE",
            "DONE, DONE",
            "DONE, OPEN"
    })
    void canTransitionTo_whenAllowed_shouldRespond(IssueStatus from, IssueStatus to) {
        assertTrue(from.canTransitionTo(to));
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, IN_REVIEW",
            "OPEN, DONE",
            "IN_PROGRESS, DONE",
            "IN_REVIEW, OPEN",
            "DONE, IN_PROGRESS",
            "DONE, IN_REVIEW"
    })
    void canTransitionTo_whenForbidden_shouldReturnFalse(IssueStatus from, IssueStatus to) {
        assertFalse(from.canTransitionTo(to));
    }
}