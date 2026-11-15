package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.BurndownEvent;
import com.example.enterprise_issues_tracking_system.dto.BurndownPointResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BurndownCalculator {

    private enum State {OPEN, DONE, DELETED}
    private record Effect(int remainingDelta, boolean created, boolean completed) {
        static final Effect None = new Effect(0, false, false);
    }
    private BurndownCalculator() {
    }
    private static String targetStatus(String details) {
        if (details == null) {
            return null;
        }
        String[] parts = details.split("->");
        if (parts.length != 2) {
            return null;
        }
        return parts[1].trim();
    }

    private static Effect apply(BurndownEvent event, Map<Long, State> states) {
        Long issueId = event.issueId();
        if (issueId == null) {
            return Effect.None;
        }
        State state = states.get(issueId);
        switch (event.action()) {
            case CREATED -> {
                if (state == null) {
                    return Effect.None;
                }
                states.put(issueId, State.OPEN);
                return new Effect(1, true, false);
            }
            case STATUS_CHANGED -> {
                if (state == null || state == State.DELETED) {
                    return Effect.None;
                }
                String target = targetStatus(event.details());
                if (target == null) {
                    return Effect.None;
                }
                if (state == State.OPEN && target.equals("DONE")) {
                    states.put(issueId, State.DONE);
                    return new Effect(-1, false, true);
                }
                if (state == State.DONE && target.equals("DONE")) {
                    states.put(issueId, State.OPEN);
                    return new Effect(1, false, false);
                }
                return Effect.None;
            }
            case DELETED -> {
                if (state == null || state == State.DELETED) {
                    return Effect.None;
                }
                states.put(issueId, State.DELETED);
                return new Effect(state == State.OPEN ? -1 : 0, false, false);
            }
            default -> {
                return Effect.None;
            }
        }
    }

    public static List<BurndownPointResponse> calculate(List<BurndownEvent> events, LocalDate from, LocalDate to) {
        Map<Long, State> states = new HashMap<>();
        long remaining = 0;
        int next = 0;
        LocalDateTime firstMoment = from.atStartOfDay();
        while (next < events.size() && events.get(next).createdAt().isBefore(firstMoment)) {
            remaining += apply(events.get(next), states).remainingDelta;
            next++;
        }
        List<BurndownPointResponse> points = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();
            long created = 0;
            long completed = 0;
            while (next < events.size() && events.get(next).createdAt().isBefore(endOfDay)) {
                Effect effect = apply(events.get(next), states);
                remaining += effect.remainingDelta;
                if (effect.created()) {
                    created++;
                }
                if (effect.completed()) {
                    completed++;
                }
                next++;
            }
            points.add(new BurndownPointResponse(day, remaining, created, completed));
        }
        return points;
    }
}
