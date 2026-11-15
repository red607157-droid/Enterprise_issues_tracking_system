package com.example.enterprise_issues_tracking_system.dto;

import java.time.LocalDate;

public record BurndownPointResponse(LocalDate date, long remaining, long created, long completed) {
}
