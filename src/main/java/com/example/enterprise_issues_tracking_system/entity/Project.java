package com.example.enterprise_issues_tracking_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Projects")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Project extends Auditable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String key;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(length = 100)
    private String description;

    @Column
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public void softDelete() {
        deletedAt = LocalDateTime.now();
    }
}
