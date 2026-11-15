package com.example.enterprise_issues_tracking_system.dto;

import java.time.LocalDate;
import java.util.List;

public record BurndownResponse(LocalDate from, LocalDate to, List<BurndownPointResponse> points) {
}
