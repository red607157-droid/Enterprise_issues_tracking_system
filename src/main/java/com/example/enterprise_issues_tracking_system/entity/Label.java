package com.example.enterprise_issues_tracking_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Labels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Builder
public class Label {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 30)
    private String color;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    public void softDelete() {
        deletedAt = LocalDateTime.now();
    }
}
