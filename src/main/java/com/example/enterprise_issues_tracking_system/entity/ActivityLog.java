package com.example.enterprise_issues_tracking_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ActivityLog {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private AuditEntityType entityType;

    @Column(nullable = false, updatable = false)
    private Long entityId;

    @Column(nullable = false, updatable = false)
    private Long projectId;

    @Column(updatable = false)
    private Long issueId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, updatable = false)
    private AuditAction action;

    @Column(nullable = false, updatable = false)
    private Long actorId;

    @Column(length = 500, updatable = false)
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}