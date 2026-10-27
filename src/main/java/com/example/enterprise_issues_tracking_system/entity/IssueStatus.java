package com.example.enterprise_issues_tracking_system.entity;

public enum IssueStatus {
    OPEN,
    IN_PROGRESS,
    IN_REVIEW,
    DONE;
    public boolean canTransitionTo(IssueStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case OPEN -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == OPEN || target == IN_REVIEW;
            case IN_REVIEW -> target == IN_PROGRESS || target == DONE;
            case DONE -> target == OPEN;
        };
    }
}
