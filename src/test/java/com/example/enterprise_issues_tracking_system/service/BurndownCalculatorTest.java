package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.BurndownEvent;
import com.example.enterprise_issues_tracking_system.dto.BurndownPointResponse;
import com.example.enterprise_issues_tracking_system.entity.AuditAction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BurndownCalculatorTest {

    private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2026, 11, 2);
    private static final LocalDate DAY_3 = LocalDate.of(2026, 11, 3);
    private static final LocalDate DAY_4 = LocalDate.of(2026, 11, 4);

    private BurndownEvent created(long issueId, LocalDate day, int hour) {
        return new BurndownEvent(issueId, AuditAction.CREATED, "title", day.atTime(hour, 0));
    }

    private BurndownEvent statusChanged(long issueId, String details, LocalDate day, int hour) {
        return new BurndownEvent(issueId, AuditAction.STATUS_CHANGED, details, day.atTime(hour, 0));
    }

    private BurndownEvent deleted(long issueId, LocalDate day, int hour) {
        return new BurndownEvent(issueId, AuditAction.DELETED, "title", day.atTime(hour, 0));
    }

    private BurndownPointResponse point(LocalDate day, long remaining, long created, long completed) {
        return new BurndownPointResponse(day, remaining, created, completed);
    }

    @Test
    void calculate_whenIssuesCreatedAndOneCompleted_shouldTrackRemainingPerDay() {
        List<BurndownEvent> events = List.of(
                created(1, DAY_1, 9),
                created(2, DAY_1, 10),
                statusChanged(1, "IN_REVIEW -> DONE", DAY_2, 12));

        List<BurndownPointResponse> result = BurndownCalculator.calculate(events, DAY_1, DAY_3);

        assertEquals(List.of(
                point(DAY_1, 2, 2, 0),
                point(DAY_2, 1, 0, 1),
                point(DAY_3, 1, 0, 0)), result);
    }

    @Test
    void calculate_whenCompletedIssueIsReopened_shouldCountItAsRemainingAgain() {
        List<BurndownEvent> events = List.of(
                created(1, DAY_1, 9),
                statusChanged(1, "IN_REVIEW -> DONE", DAY_1, 15),
                statusChanged(1, "DONE -> OPEN", DAY_2, 9));

        List<BurndownPointResponse> result = BurndownCalculator.calculate(events, DAY_1, DAY_2);

        assertEquals(List.of(
                point(DAY_1, 0, 1, 1),
                point(DAY_2, 1, 0, 0)), result);
    }

    @Test
    void calculate_whenIssuesAreDeleted_shouldOnlyReduceRemainingForOpenOnes() {
        List<BurndownEvent> events = List.of(
                created(1, DAY_1, 9),
                created(2, DAY_1, 10),
                statusChanged(2, "IN_REVIEW -> DONE", DAY_2, 9),
                deleted(1, DAY_3, 9),
                deleted(2, DAY_4, 9));

        List<BurndownPointResponse> result = BurndownCalculator.calculate(events, DAY_1, DAY_4);

        assertEquals(List.of(
                point(DAY_1, 2, 2, 0),
                point(DAY_2, 1, 0, 1),
                point(DAY_3, 0, 0, 0),
                point(DAY_4, 0, 0, 0)), result);
    }

    @Test
    void calculate_whenEventsHappenedBeforeTheRange_shouldStartFromTheirResult() {
        List<BurndownEvent> events = List.of(
                created(1, LocalDate.of(2026, 10, 30), 9),
                created(2, LocalDate.of(2026, 10, 30), 10),
                statusChanged(2, "IN_REVIEW -> DONE", LocalDate.of(2026, 10, 31), 9));

        List<BurndownPointResponse> result = BurndownCalculator.calculate(events, DAY_1, DAY_2);

        assertEquals(List.of(
                point(DAY_1, 1, 0, 0),
                point(DAY_2, 1, 0, 0)), result);
    }

    @Test
    void calculate_whenEventsAreAfterTheRange_shouldIgnoreThem() {
        List<BurndownEvent> events = List.of(created(1, DAY_3, 9));

        List<BurndownPointResponse> result = BurndownCalculator.calculate(events, DAY_1, DAY_2);

        assertEquals(List.of(point(DAY_1, 0, 0, 0), point(DAY_2, 0, 0, 0)), result);
    }

    @Test
    void calculate_whenIssueHasNoCreatedEvent_shouldIgnoreItsOtherEvents() {
        List<BurndownEvent> events = List.of(
                statusChanged(9, "IN_REVIEW -> DONE", DAY_1, 9),
                deleted(9, DAY_1, 10));

        List<BurndownPointResponse> result = BurndownCalculator.calculate(events, DAY_1, DAY_1);

        assertEquals(List.of(point(DAY_1, 0, 0, 0)), result);
    }

    @Test
    void calculate_whenStatusChangeDoesNotTouchDone_shouldChangeNothing() {
        List<BurndownEvent> events = List.of(
                created(1, DAY_1, 9),
                statusChanged(1, "OPEN -> IN_PROGRESS", DAY_1, 10),
                statusChanged(1, "IN_PROGRESS -> IN_REVIEW", DAY_1, 11));

        List<BurndownPointResponse> result = BurndownCalculator.calculate(events, DAY_1, DAY_1);

        assertEquals(List.of(point(DAY_1, 1, 1, 0)), result);
    }

    @Test
    void calculate_whenDetailsAreMissingOrMalformed_shouldIgnoreTheEvent() {
        List<BurndownEvent> events = List.of(
                created(1, DAY_1, 9),
                statusChanged(1, null, DAY_1, 10),
                statusChanged(1, "DONE", DAY_1, 11),
                statusChanged(1, "A -> B -> DONE", DAY_1, 12));

        List<BurndownPointResponse> result = BurndownCalculator.calculate(events, DAY_1, DAY_1);

        assertEquals(List.of(point(DAY_1, 1, 1, 0)), result);
    }

    @Test
    void calculate_whenThereAreNoEvents_shouldReturnOneZeroPointPerDay() {
        List<BurndownPointResponse> result = BurndownCalculator.calculate(List.of(), DAY_1, DAY_3);

        assertEquals(List.of(point(DAY_1, 0, 0, 0), point(DAY_2, 0, 0, 0), point(DAY_3, 0, 0, 0)), result);
    }

    @Test
    void calculate_whenEventsShareTheSameDay_shouldReplayThemInOrder() {
        List<BurndownEvent> events = List.of(
                created(1, DAY_1, 8),
                statusChanged(1, "IN_REVIEW -> DONE", DAY_1, 12),
                statusChanged(1, "DONE -> IN_PROGRESS", DAY_1, 16));

        List<BurndownPointResponse> result = BurndownCalculator.calculate(events, DAY_1, DAY_1);

        assertEquals(List.of(point(DAY_1, 1, 1, 1)), result);
    }
}